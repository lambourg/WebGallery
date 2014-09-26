import os.path
import subprocess
from xml.etree import ElementTree

from django.conf import settings
from django.db import models
from django.db.models.signals import pre_delete
from django.dispatch import receiver
from django.utils.http import urlquote

from PIL import Image, ExifTags

from webgallery.exif_utility import read_exifs

from mediacontroller import add_constructor
from media import Media

ORIENTATION_TAG = None
for tag in ExifTags.TAGS:
    if ExifTags.TAGS[tag] == 'Orientation':
        ORIENTATION_TAG = tag
        break

SIZE_ORIGINAL = 'original'


class Picture(Media):
    def __unicode__(self):
        return self.folder.relative_path + '/' + self.basename

    @staticmethod
    def get_or_create(folder, basename):
        obj, created = Picture.objects.get_or_create(
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
        return Picture.objects.get(pk=int(uid))

    def full_resized_path(self, size_name):
        """ Full resized path, as utf-8
        :param size_name:
        :return:
        """
        return os.path.join(
            self.folder.full_resized_path,
            size_name,
            self.basename)

    def get_or_create_resized(self, size_name):
        w, h = self.__get_size(size_name)
        obj, created = self.resized_set.get_or_create(
            size_name=size_name,
            defaults={'width': w, 'height': h})

        if created:
            obj.do_update()

        return obj

    @property
    def thumb(self):
        thumb_size_name = settings.WG_THUMB_NAME
        return self.get_or_create_resized(thumb_size_name)

    @property
    def available_sizes(self):
        size_names = settings.WG_SIZE_NAMES
        sizes = settings.WG_SIZES
        ret = []
        for key in size_names:
            width, height = sizes[key]

            if width >= self.width and height >= self.height:
                break

            ret.append(key)

        ret.append(SIZE_ORIGINAL)

        return ret

    def __get_size(self, size_name):
        if size_name == SIZE_ORIGINAL:
            return self.width, self.height

        width, height = settings.WG_SIZES[size_name]

        ratio_w = float(self.width) / float(width)
        ratio_h = float(self.height) / float(height)
        ratio = max(ratio_w, ratio_h)

        return int(round(self.width / ratio)), int(round(self.height / ratio))

    @staticmethod
    def get_orientation_code(img):
        exif_data = img._getexif()
        orientation = 1
        if exif_data is not None:
            exif = dict(exif_data.items())
            if ORIENTATION_TAG in exif.keys():
                orientation = exif[ORIENTATION_TAG]
        return orientation

    def do_update(self):
        full = self.full_original_path

        if not os.path.exists(full):
            self.delete()
            return True

        updated = False
        current_timestamp = os.path.getmtime(full)

        if current_timestamp != self.timestamp:
            self.timestamp = current_timestamp
            updated = True

            # ############################
            # update width and height

            im = Image.open(full)
            self.width, self.height = im.size
            orientation = Picture.get_orientation_code(im)
            if orientation == 6 or orientation == 8:
                # image is rotated +-90 degrees
                temp = self.width
                self.width = self.height
                self.height = temp

            #############################
            # update the EXIF values

            self.exif_set.all().delete()
            exifs = read_exifs(full)

            for key in exifs.keys():
                self.exif_set.create(
                    key=key,
                    value=exifs[key])

        #############################
        # update the resized pictures

        thumb_size_name = settings.WG_THUMB_NAME
        sizes = [thumb_size_name] + self.available_sizes

        for size_name in sizes:
            resized = self.resized_set.filter(size_name=size_name)
            w, h = self.__get_size(size_name)

            if not resized.exists():
                obj = self.resized_set.create(
                    size_name=size_name,
                    width=w,
                    height=h)
            else:
                obj = resized[0]
            obj.do_update(propagate=False)

        for resized in self.resized_set.all():
            if resized.size_name not in sizes:
                resized.delete()

        if self.update_visibility(autosave=False):
            updated = True

        if updated:
            self.save()

        return updated

    def update_visibility(self, autosave=True):
        thumb = self.thumb
        visible = not thumb.to_update

        if self.visible != visible:
            self.visible = visible

            if autosave:
                self.save()

            return True
        else:
            return False

    def as_xml(self, parent):
        elt = ElementTree.SubElement(parent, 'picture')
        elt.set('thumb', self.thumb.url)
        elt.set('fileid', str(self.id))
        elt.set('name', self.name)
        elt.set('folder', u'/%s' % self.folder.relative_path)

        sizes = ElementTree.SubElement(elt, 'sizes')
        for key in self.available_sizes:
            child = ElementTree.SubElement(sizes, 'size')
            child.set('name', key)
            resized = self.get_or_create_resized(key)
            child.set('width', str(resized.width))
            child.set('height', str(resized.height))
            child.set('url', resized.url)

        exifs = ElementTree.SubElement(elt, 'exifs')
        for exif in self.exif_set.iterator():
            child = ElementTree.SubElement(exifs, 'exif')
            child.set('key', exif.key)
            child.set('value', exif.value)

        return elt

    def ensure_size(self, size_name):
        """Makes sure that the thumbnail with size size_name exists"""
        resized = self.resized_set.filter(size_name=size_name)
        if not resized.exists():
            return False

        resized = resized[0]
        if resized.to_update:
            resized.do_resize()
            resized.do_update()
        return True


class Exif(models.Model):
    picture = models.ForeignKey(Picture)
    key = models.CharField(max_length=200)
    value = models.CharField(max_length=200)

    def __unicode__(self):
        return u'%s (%s)' % (self.key, unicode(self.picture))


class Resized(models.Model):
    picture = models.ForeignKey(Picture)
    size_name = models.CharField(max_length=40)
    width = models.IntegerField()
    height = models.IntegerField()
    to_update = models.BooleanField(default=True)

    def __unicode__(self):
        return u'%s (%s)' % (self.size_name, unicode(self.picture))

    @property
    def max_size(self):
        if self.size_name == SIZE_ORIGINAL:
            return -1, -1
        else:
            return settings.WG_SIZES[self.size_name]

    @property
    def full_path(self):
        return self.picture.full_resized_path(self.size_name)

    @property
    def url(self):
        return(u'%s/%s/%s/%s' % (
            settings.WG_RESIZED_URL,
            urlquote(self.picture.folder.relative_path),
            self.size_name,
            urlquote(self.picture.basename)))

    def do_update(self, propagate=False, autosave=True):
        src = self.picture.full_original_path
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

        if modified and self.size_name == settings.WG_THUMB_NAME:
            updated = self.picture.update_visibility(autosave=autosave)
            if updated and propagate:
                self.picture.folder.update_visibility(propagate=True)

    @property
    def use_multiproc(self):
        return True

    def resize_args(self):
        src = self.picture.full_original_path
        dest = self.full_path

        dirname = os.path.dirname(self.full_path)
        if not os.path.exists(dirname):
            os.makedirs(dirname)

        args = ['convert', src,
                '-auto-orient',
                '-sharpen', '0x1',
                '-quality', '75']

        if self.size_name != SIZE_ORIGINAL:
            w, h = settings.WG_SIZES[self.size_name]
            args += [
                '-resize', '%dx%d' % (w, h),
                '-filter', 'triangle']

        args.append(dest)

        return args

    def do_resize(self):
        try:
            subprocess.check_call(self.resize_args())
        except subprocess.CalledProcessError:
            if os.path.exists(self.full_path):
                os.unlink(self.full_path)


@receiver(pre_delete, sender=Resized)
def on_delete(sender, instance, using, **kwargs):
    full = instance.full_path
    if os.path.exists(full):
        print 'unlink %s' % full
        os.remove(full)


add_constructor(['.jpg', '.jpeg'], Picture.get_or_create)