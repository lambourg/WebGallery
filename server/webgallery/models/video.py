import os
import os.path
import re
import subprocess
from xml.etree import ElementTree

from django.conf import settings
from django.contrib.auth.models import User
from django.db import models
from django.db.models.signals import pre_delete
from django.dispatch import receiver
from django.utils.http import urlquote

from mediacontroller import add_constructor
from media import Media


FFPROBE = 'ffprobe'
FFMPEG = 'ffmpeg'
SIZE_ORIGINAL = 'original'

# RE_STREAM
# picks up the Streams descriptions, and catches in groups:
# 1: the stream id
# 2: the stream's language
# 3: the kind of stream (Video/Audio/...)
# 4: the stream codec
RE_STREAM = re.compile(
    r'^Stream #(0[:.][0-9]+)(?:\(([^)]+)\))?: ([^:]+): ([0-9a-z_]+)(?: |,).*')

# RE_SIZE
# in case of Stream definition line for Video, picks up the video size
RE_SIZE = re.compile(
    r'^.*, ([0-9]+)x([0-9]+)(?:, | [[]).*')

RE_META = re.compile(
    r'^([^ ]+) *: (.*)')


def ffprobe(infile):
    """Returns informations on the streams contained in 'infile'"""

    # run ffprobe on file
    args = [FFPROBE, infile]
    out = subprocess.check_output(args, stderr=subprocess.STDOUT)
    tracks = []

    for line in out.splitlines():
        line = line.strip()
        # only keep Stream definition line
        match = RE_STREAM.match(line)

        if match is None:
            continue

        lang = match.group(2)

        if lang is None:
            lang = 'unk'

        track = {
            'stream': match.group(1),
            'lang': lang,
            'kind': match.group(3),
            'codec': match.group(4)}

        if track['kind'] == 'Video':
            match = RE_SIZE.match(line)
            track['width'] = int(match.group(1))
            track['height'] = int(match.group(2))

        tracks.append(track)
    return tracks


def get_metadata(infile):
    args = [FFPROBE, infile]
    out = subprocess.check_output(args, stderr=subprocess.STDOUT)
    in_metadata = False
    ret = []

    for line in out.splitlines():
        line = line.strip()
        if line == 'Metadata:':
            in_metadata = True
        elif in_metadata:
            match = RE_META.match(line)
            if match is not None:
                key = match.group(1)
                val = match.group(2)

                if key == 'Duration':
                    match = re.match(r'([0-9:]+)[.].*', val)
                    val = match.group(1)

                    ret.append((key, val))

                    return ret

                elif key == 'creation_time':
                    ret.append(('Date', val))

                elif key == 'album_artist':
                    ret.append(('Author', val))

    return []


def thumb(infile, outfile, time, width, height):
    dirname = os.path.dirname(outfile)
    if not os.path.exists(dirname):
        os.makedirs(dirname)
    args = [FFMPEG,
            '-ss', str(time),
            '-i', infile,
            '-frames:v', '1',
            '-vf', 'scale=%d:%d' % (width, height),
            outfile]
    try:
        subprocess.check_call(args)
    except subprocess.CalledProcessError:
        if os.path.exists(outfile):
            os.unlink(outfile)


class Video(Media):
    def __unicode__(self):
        return self.folder.relative_path + '/' + self.basename

    @staticmethod
    def get_or_create(folder, basename):
        obj, created = Video.objects.get_or_create(
            basename=basename,
            folder=folder,
            defaults={'timestamp': None, 'width': 0, 'height': 0})
        if created:
            obj.do_update()

        return obj

    @staticmethod
    def get(uid):
        """returns the Folder given its id

        :param uid: the Folder's id
        :return: a Folder
        """
        return Video.objects.get(pk=int(uid))

    @property
    def __available_sizes(self):
        size_names = settings.WG_VIDEO_SIZE_NAMES
        sizes = settings.WG_VIDEO_SIZES
        ret = []
        for key in size_names:
            height = sizes[key]

            if height >= self.height:
                break

            ret.append(key)

        ret.append(SIZE_ORIGINAL)

        return ret

    def __get_size(self, size_name):
        if size_name == SIZE_ORIGINAL:
            return self.width, self.height
        height = settings.WG_VIDEO_SIZES[size_name]
        # width MUST be a multiple of 2
        width = int(self.width * height / self.height / 2) * 2
        return width, height

    def full_resized_path(self, size_name):
        """Full resized path as unicode string"""
        resized = self.folder.full_resized_path
        base, ext = os.path.splitext(self.basename)
        return os.path.join(resized, size_name, '%s.mp4' % base)

    @property
    def __thumb_url(self):
        """thumbnail url as unicode string"""
        base, ext = os.path.splitext(self.basename)
        return u'%s/%s/%s/%s.jpg' % (
            settings.WG_RESIZED_URL,
            urlquote(self.folder.relative_path),
            settings.WG_THUMB_NAME,
            urlquote(base))

    @property
    def __thumb_path(self):
        """Thumbnail path as unicode string"""
        resized = self.folder.full_resized_path
        base, ext = os.path.splitext(self.basename)
        return os.path.join(resized, settings.WG_THUMB_NAME, u'%s.jpg' % base)

    def do_update(self):
        full = self.full_original_path

        if not os.path.exists(full):
            self.delete()
            return True

        updated = False
        current_timestamp = os.path.getmtime(full)

        if current_timestamp != self.timestamp:
            updated = True
            self.timestamp = current_timestamp

            tracks = ffprobe(full)
            for t in tracks:
                if t['kind'] == 'Video':
                    self.width = t['width']
                    self.height = t['height']

            #############################
            # update the EXIF values

            metadata = get_metadata(full)
            self.videoexif_set.all().delete()
            for key, val in metadata:
                self.videoexif_set.create(key=key, value=val)

        #############################
        # create/update thumbnail
        thumb_path = self.__thumb_path
        if not os.path.exists(thumb_path) or \
                os.path.getmtime(thumb_path) < current_timestamp:
            if os.path.exists(thumb_path):
                os.unlink(thumb_path)
            hh, mm, ss = \
                self.videoexif_set.get(key='Duration').value.split(':')
            total = int(hh) * 3600 + int(mm) * 60 + int(ss)
            snaptime = int(total / 10)
            h = settings.WG_SIZES[settings.WG_THUMB_NAME][1]
            w = self.width * h / self.height

            thumb(self.full_original_path, thumb_path, snaptime, w, h)

        #############################
        # update the resized videos

        sizes = self.__available_sizes

        for size_name in sizes:
            w, h = self.__get_size(size_name)
            resized, created = self.videoresized_set.get_or_create(
                size_name=size_name,
                defaults={'width': w, 'height': h})

            resized.do_update(propagate=False)

        for resized in self.videoresized_set.all():
            if resized.size_name not in sizes:
                resized.delete()

        if self.update_visibility(autosave=False):
            updated = True

        if updated:
            self.save()

    def update_visibility(self, autosave=True):
        visible = self.videoresized_set.filter(to_update=False).exists()

        if visible != self.visible:
            self.visible = visible
            if autosave:
                self.save()

            return True
        else:
            return False

    def as_xml(self, parent):
        elt = ElementTree.SubElement(parent, 'video')
        elt.set('thumb', self.__thumb_url)
        elt.set('fileid', str(self.id))
        elt.set('name', self.name)
        elt.set('folder', u'/%s' % self.folder.relative_path)

        sizes = ElementTree.SubElement(elt, 'sizes')
        for size_name in self.__available_sizes:
            try:
                resized = self.videoresized_set.get(
                    size_name=size_name, to_update=False)
            except VideoResized.DoesNotExist:
                continue
            child = ElementTree.SubElement(sizes, 'size')
            child.set('name', size_name)
            child.set('width', str(resized.width))
            child.set('height', str(resized.height))
            child.set('url', resized.url)

        exifs = ElementTree.SubElement(elt, 'exifs')
        for exif in self.videoexif_set.iterator():
            child = ElementTree.SubElement(exifs, 'exif')
            child.set('key', exif.key)
            child.set('value', exif.value)

        return elt


class VideoExif(models.Model):
    video = models.ForeignKey(Video)
    key = models.CharField(max_length=200)
    value = models.CharField(max_length=200)

    def __unicode__(self):
        return u'%s (%s)' % (self.key, unicode(self.video))


class VideoResized(models.Model):
    video = models.ForeignKey(Video)
    size_name = models.CharField(max_length=40)
    width = models.IntegerField()
    height = models.IntegerField()
    to_update = models.BooleanField(default=True)

    def __unicode__(self):
        return u'%s (%s)' % (self.size_name, unicode(self.video))

    @property
    def full_path(self):
        return self.video.full_resized_path(self.size_name)

    @property
    def url(self):
        base, ext = os.path.splitext(self.video.basename)
        return(u'%s/%s/%s/%s.mp4' % (
            settings.WG_RESIZED_URL,
            urlquote(self.video.folder.relative_path),
            self.size_name,
            urlquote(base)))

    def do_update(self, propagate=False, autosave=True):
        src = self.video.full_original_path
        dest = self.full_path
        modified = False

        if not os.path.exists(dest):
            to_update = True
        elif os.path.getmtime(src) > os.path.getmtime(dest):
            to_update = True
        else:
            to_update = False

        if to_update != self.to_update:
            modified = True
            self.to_update = to_update
            self.save()

        if modified:
            self.video.update_visibility(autosave=autosave)
        if propagate:
            self.video.folder.update_visibility(propagate=True)

    @property
    def use_multiproc(self):
        return False

    def resize_args(self):
        full = self.video.full_original_path
        tracks = ffprobe(full)
        args = [FFMPEG, '-loglevel', 'error', '-stats', '-i', full]

        for t in tracks:
            if t['kind'] == 'Video':
                args += ['-map', t['stream'],
                         '-c:v', 'libx264',
                         '-crf', '25',
                         '-tune', 'film',
                         '-profile:v', 'main', '-level', '4']
                if self.size_name != SIZE_ORIGINAL:
                    args += ['-vf',
                             'scale=%d:%d' % (
                                 int(self.width), int(self.height))]

            elif t['kind'] == 'Audio':
                args += ['-map', t['stream'], '-c:a', 'libfdk_aac']

        args += [self.full_path]
        print ' '.join(args)

        base = os.path.dirname(self.full_path)
        if not os.path.exists(base):
            os.makedirs(base)
        if os.path.exists(self.full_path):
            os.unlink(self.full_path)

        return args


@receiver(pre_delete, sender=VideoResized)
def on_delete(sender, instance, using, **kwargs):
    full = instance.full_path
    if os.path.exists(full):
        print 'unlink %s' % full
        os.remove(full)


add_constructor(['.avi', '.mov', '.mp4', '.m4v'], Video.get_or_create)
