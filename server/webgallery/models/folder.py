from django.conf import settings
from django.db import models
from django.db import transaction
from django.utils.http import urlquote
from django.utils.encoding import smart_text

from itertools import chain
import os
import os.path
import shutil
import StringIO
import time
import zipfile

from xml.etree import ElementTree

from mediacontroller import get_or_create


class Folder(models.Model):
    parent = models.ForeignKey('self', blank=True, null=True)
    relative_path = models.CharField(max_length=1024)
    last_updated = models.IntegerField(default=-1)
    visible = models.BooleanField(default=False)
    xml = models.TextField(default="")

    def __unicode__(self):
        return self.relative_path

    @staticmethod
    def get_root():
        return Folder.get_or_create(None, "")

    @staticmethod
    def get_or_create(parent, relative_path):
        obj, created = Folder.objects.get_or_create(
            relative_path=relative_path,
            defaults={'parent': parent, 'relative_path': relative_path})
        if created and settings.DEBUG:
            print u"NEW FOLDER DETECTED: %s" % relative_path

        return obj

    @staticmethod
    def get(uid):
        """returns the Folder given its id

        :param uid: the Folder's id
        :return: a Folder
        """
        return Folder.objects.get(pk=int(uid))

    @property
    def full_original_path(self):
        """Directory full path, as unicode"""
        base = settings.WG_PICTURES_PATH
        return os.path.join(
            base, smart_text(self.relative_path))

    @property
    def full_resized_path(self):
        """Directory's resized path, as unicode"""
        base = settings.WG_RESIZED_PATH
        return os.path.join(
            base, smart_text(self.relative_path))

    @property
    def zip_filename(self):
        """Zip file name, as unicode"""
        return unicode(
            os.path.join(
                self.full_resized_path,
                u'%s.zip' % os.path.basename(smart_text(self.relative_path))))

    def ensure_zip(self):
        path = self.zip_filename
        compute = False

        if not os.path.exists(path):
            compute = True

        elif os.path.getmtime(path) < self.last_updated:
            compute = True

        if not compute:
            return

        # Let's create the zip file
        if not os.path.exists(self.full_resized_path):
            os.makedirs(self.full_resized_path)

        print u"create archive %s" % path

        with open(path, 'w') as f:
            archive = zipfile.ZipFile(f, 'w', allowZip64=True)

            for media in self.picture_set.all():
                full = media.full_original_path
                archive.write(full, media.basename)
            for media in self.video_set.all():
                full = media.full_original_path
                archive.write(full, media.basename)

            archive.close()

    def set_as_updated(self):
        self.last_updated = time.time()

    @property
    def name(self):
        """Folder's name, as unicode string"""
        if len(unicode(self.relative_path)) == 0:
            return '/'
        else:
            return os.path.basename(
                unicode(self.relative_path)).replace('_', ' ')

    def xml_get_directories(self, parent=None):
        """Returns an XML representation of the folder, as
         xml.etree.ElementTree

        :param parent: the parent element if any, or None
        :return: an xml.etree.ElementTree.Element
        """
        if parent is not None:
            elt = ElementTree.SubElement(parent, "folder")
        else:
            elt = ElementTree.Element("folder")

        elt.set('name', self.name)
        elt.set('id', str(self.id))
        elt.set('url', urlquote(self.relative_path))

        if self.picture_set.exists() or self.video_set.exists():
            elt.set('haspictures', '1')
        else:
            elt.set('haspictures', '0')

        folder_set = self.folder_set.filter(visible=True)
        for d in folder_set.order_by('relative_path'):
            d.xml_get_directories(elt)

        return elt

    def xml_get_pictures(self):
        # We use a cache here to return quickly the xml view of the folder
        if self.update_xml(False):
            self.save()

        return self.xml

    def delete(self, using=None):
        print u"FOLDER DELETED: %s" % self.relative_path
        full = self.full_resized_path
        if os.path.exists(full):
            shutil.rmtree(full)
        super(Folder, self).delete(using)

    def do_update(self):
        if not os.path.exists(self.full_original_path):
            with transaction.atomic():
                self.delete()
            return

        # This pass checks for deletion or updates of sub-files and directories
        basenames = []

        for f in self.folder_set.order_by('-relative_path'):
            basename = os.path.basename(f.relative_path)
            if basename in basenames:
                with transaction.atomic():
                    f.delete()
            else:
                f.do_update()
                basenames.append(os.path.basename(f.relative_path))

        for f in chain(self.picture_set.order_by('basename'),
                       self.video_set.order_by('basename')):
            try:
                self.has_error = True
                with transaction.atomic():
                    if f.do_update():
                        self.set_as_updated()
                basenames.append(f.basename)

            except os.EX_OSERR:
                print u"error catched while updating %s/%s" % (
                    self.relative_path, f.basename)
                # A file has been deleted ? Let's retry a full update of
                # the directory
                if not self.has_error:
                    self.do_update()
                    return
                else:
                    raise

        # Now check for new files or directories

        for f in os.listdir(self.full_original_path):
            if f.startswith('.'):
                continue

            f = smart_text(f)

            if f in basenames:
                # already updated
                continue

            full = os.path.join(self.full_original_path, f)

            if os.path.isdir(full):
                relative_path = os.path.join(unicode(self.relative_path), f)
                with transaction.atomic():
                    subdir = Folder.get_or_create(self, relative_path)
                subdir.do_update()

            if os.path.isfile(full):
                with transaction.atomic():
                    obj = get_or_create(self, f)
                    if obj is not None:
                        self.set_as_updated()

        self.update_visibility(propagate=False)
        self.ensure_zip()

    def update_visibility(self, propagate=False):
        modified = self.update_xml(force=True)

        if self.folder_set.filter(visible=True).exists():
            newval = True
        elif self.picture_set.filter(visible=True).exists():
            newval = True
        elif self.video_set.filter(visible=True).exists():
            newval = True
        else:
            newval = False

        if newval != self.visible:
            self.visible = newval
            # need to save before propagating to parent
            self.save()

            print u"'%s': visibile is '%s'" % (
                self.relative_path, str(self.visible))

            if propagate and self.parent is not None:
                self.parent.update_visibility(propagate=True)

            modified = True

        elif modified:
            self.save()

        return modified

    def update_xml(self, force):
        if not force and len(self.xml) > 0:
            return False
        root = ElementTree.Element("thumbs")
        root.set('dirname', self.name)
        root.set('dirid', str(self.id))

        result_list = sorted(
            chain(self.picture_set.filter(visible=True),
                  self.video_set.filter(visible=True)),
            key=lambda inst: inst.basename)
        for f in result_list:
            f.as_xml(root)

        tree = ElementTree.ElementTree(root)
        f = StringIO.StringIO()
        tree.write(f, xml_declaration=True)
        text = f.getvalue()

        if text != str(self.xml):
            self.xml = text
            return True

        return False
