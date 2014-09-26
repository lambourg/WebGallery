import folder
import media
import picture
import video
import comment

from django.contrib import admin

admin.site.register(folder.Folder)
admin.site.register(picture.Picture)
admin.site.register(picture.Resized)
admin.site.register(picture.Exif)
admin.site.register(video.Video)
admin.site.register(video.VideoResized)
admin.site.register(video.VideoExif)
admin.site.register(comment.Comment)