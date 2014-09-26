from django.conf import settings
from django.core.servers.basehttp import FileWrapper
from django.db import transaction
from django.http import \
    HttpResponse, HttpResponseNotFound, HttpResponseBadRequest
from django.shortcuts import render
from django.views.decorators.cache import never_cache

import StringIO
import os.path
import pytz
from xml.etree import ElementTree

from webgallery.models.folder import Folder
from webgallery.models.picture import Picture
from webgallery.models.video import Video


@transaction.atomic
@never_cache
def get_folders(request):
    """Handles the 'getdirectories' request

    :param request: the request
    :return: an HttpResponse
    """
    if 'user' in dir(request):
        print "User logged in: %s (%s <%s>)" % (
            request.user.username,
            request.user.get_full_name(),
            request.user.email)
    else:
        print dir(request)

    tz = request.GET['tz']
    if tz is not None:
        request.session['django_timezone'] = tz

    root = Folder.get_root()
    tree = ElementTree.ElementTree(root.xml_get_directories())
    f = StringIO.StringIO()
    tree.write(f, xml_declaration=True)
    return HttpResponse(f.getvalue(), content_type='application/xml')


@transaction.atomic
@never_cache
def get_folder(request, dir_id):
    """Handles the 'getpictures' request

    :param request: the request
    :return: an HttpResponse
    """
    folder = Folder.get(dir_id)

    if folder is None:
        return HttpResponseNotFound('cannot get dir with id %s' % dir_id)

    return HttpResponse(
        folder.xml_get_pictures(), content_type='application/xml')


def download_folder(request, dir_id):
    folder = Folder.get(dir_id)

    if folder is None:
        return HttpResponseNotFound('cannot get folder with id %s' % dir_id)

    folder.ensure_zip()

    wrapper = FileWrapper(file(folder.zip_filename))
    response = HttpResponse(wrapper, content_type='application/zip')
    response['Content-Disposition'] = \
        'attachment; filename=%s' % os.path.basename(folder.zip_filename)
    response['Content-Length'] = os.path.getsize(folder.zip_filename)
    response['Content-Transfer-Encoding'] = 'binary'

    return response


def user_timezone(request):
    tz = request.session.get('django_timezone')
    if tz is not None:
        return pytz.timezone(tz)
    else:
        return pytz.timezone('UTC')

def picture_info(request, picture):
    tree = picture.get_infos(request.user, user_timezone(request))
    f = StringIO.StringIO()
    tree.write(f, xml_declaration=True)
    return HttpResponse(f.getvalue(), content_type='application/xml')


@never_cache
def get_picture_info(request, file_id):
    media = request.GET['media']
    if media == 'picture':
        picture = Picture.get(file_id)
    else:
        picture = Video.get(file_id)

    if picture is None:
        return HttpResponseNotFound(
            'cannot get picture with id %s' % file_id)

    return picture_info(request, picture)


@never_cache
def like_picture(request, file_id):
    media = request.GET['media']
    if media == 'picture':
        print "get picture"
        picture = Picture.get(file_id)
    else:
        print "get video"
        picture = Video.get(file_id)

    if picture is None:
        return HttpResponseNotFound(
            'cannot get picture with id %s' % file_id)

    like = request.GET['like'] == 'true'
    picture.set_like(request.user, like)

    return picture_info(request, picture)


@transaction.atomic
@never_cache
def comment_picture(request, file_id):
    media = request.GET['media']
    if media == 'picture':
        picture = Picture.get(file_id)
    else:
        picture = Video.get(file_id)

    if picture is None:
        return HttpResponseNotFound(
            'cannot get picture with id %s' % file_id)

    content = request.read()
    if len(content) == 0:
        return HttpResponseBadRequest(
            'no content is attached to the request')

    comment = picture.comment_set.create(author=request.user, text=content)
    comment.save()

    return picture_info(request, picture)


@transaction.atomic
@never_cache
def ensure_picture(request, file_id):
    picture = Picture.get(file_id)

    if picture is None:
        return HttpResponseNotFound(
            'cannot get picture with id %s' % file_id)

    size_name = request.GET['size_name']

    if not picture.ensure_size(size_name):
        return HttpResponseNotFound(
            'picture has no resized file %s' % (
                picture.full_resized_path(size_name)))
    return HttpResponse('<ok/>', content_type='application/xml')


def download_picture(request, file_id):
    media = request.GET['media']
    if media == 'picture':
        picture = Picture.get(file_id)
    else:
        picture = Video.get(file_id)

    if picture is None:
        return HttpResponseNotFound(
            'cannot get picture with id %s' % file_id)

    path = picture.full_original_path

    wrapper = FileWrapper(file(path))
    response = HttpResponse(wrapper, content_type='application/octet-stream')
    response['Content-Disposition'] = \
        'attachment; filename=%s' % picture.basename
    response['Content-Length'] = os.path.getsize(path)
    response['Content-Transfer-Encoding'] = 'binary'

    return response
