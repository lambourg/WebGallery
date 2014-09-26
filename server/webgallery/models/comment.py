from django.contrib.auth.models import User
from django.db import models

from picture import Picture
from video import Video


class Comment(models.Model):
    picture = models.ForeignKey(Picture, null=True)
    video = models.ForeignKey(Video, null=True)
    author = models.ForeignKey(User, related_name='+')
    date = models.DateTimeField(auto_now=True)
    text = models.TextField()

    def __unicode__(self):
        if self.picture is not None:
            return u'%s (%s)' % (unicode(self.author), unicode(self.picture))
        else:
            return u'%s (%s)' % (unicode(self.author), unicode(self.video))