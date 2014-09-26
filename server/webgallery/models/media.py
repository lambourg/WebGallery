import os.path
from xml.etree import ElementTree

from django.contrib.auth.models import User
from django.db import models

from folder import Folder


class Media(models.Model):
    folder = models.ForeignKey(Folder)
    timestamp = models.IntegerField(blank=True, null=True)
    basename = models.CharField(max_length=255)
    width = models.IntegerField()
    height = models.IntegerField()
    visible = models.BooleanField(default=False)
    identified = models.ManyToManyField(User, related_name='%(app_label)s_%(class)s_identified')
    likes = models.ManyToManyField(User, related_name='liked_%(class)s')

    class Meta:
        abstract = True

    @property
    def full_original_path(self):
        """Full path of the original media file, as unicode"""
        return os.path.join(
            self.folder.full_original_path,
            self.basename)

    @property
    def name(self):
        """Media name, as unicode string"""
        base, ext = os.path.splitext(unicode(self.basename))
        return base.replace('_', ' ')

    def set_like(self, user, like):
        people = self.likes.filter(username=user.username)
        if like:
            if people.exists():
                return
            self.likes.add(user)
            self.save()
            print 'like added: %s' % str(self.likes.all())
        else:
            if not people.exists():
                return
            self.likes.remove(user)
            self.save()
            print 'like removed: %s' % str(self.likes.all())

    def get_infos(self, user, tz):
        elt = ElementTree.Element('picture_info')
        likes = ElementTree.SubElement(elt, 'likes')
        likes.set('includes_me', "false")
        likes.set('total', str(self.likes.count()))
        for people in self.likes.all():
            if people.username == user.username:
                likes.set('includes_me', "true")
            else:
                child = ElementTree.SubElement(likes, 'people')
                child.set('full_name', people.get_full_name())
                child.set('login', people.username)

        identifications = ElementTree.SubElement(elt, 'identified_people')
        identifications.set('includes_me', "false")
        for people in self.identified.all():
            if people.username == user.username:
                identifications.set('includes_me', "true")
            else:
                child = ElementTree.SubElement(identifications, 'people')
                child.set('full_name', user.get_full_name())

        comments = ElementTree.SubElement(elt, 'comments')
        for comment in self.comment_set.all():
            child = ElementTree.SubElement(comments, 'comment')
            child.set(
                'author', comment.author.get_full_name())
            child.set(
                'date', comment.date.astimezone(tz).strftime('%Y-%m-%d %H:%M'))
            delete_allowed = str(user.username == comment.author.username)
            child.set(
                'delete-allowed', delete_allowed)
            child.text = comment.text

        return ElementTree.ElementTree(elt)
