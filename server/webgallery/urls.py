from django.conf.urls import patterns, include, url
from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.views.generic import TemplateView

from webgallery.views import get_folders, get_folder, \
    get_picture_info, like_picture, comment_picture, ensure_picture, \
    download_folder, download_picture

urlpatterns = patterns(
    '',
    # Examples:
    # url(r'^$', 'webgallery.views.home', name='home'),
    # url(r'^blog/', include('blog.urls')),

    url(r'^admin/', include(admin.site.urls)),
    url(r'^$', TemplateView.as_view(template_name='index.html')),
    url(r'^folders/$', get_folders),
    url(r'^folders/(\d+)/$', get_folder),
    url(r'^folders/(\d+)/download/$', download_folder),
    url(r'^files/(\d+)/infos/$', get_picture_info),
    url(r'^files/(\d+)/like/$', like_picture),
    url(r'^files/(\d+)/comment/$', comment_picture),
    url(r'^files/(\d+)/ensure/$', ensure_picture),
    url(r'^files/(\d+)/download/$', download_picture),) + \
              static('/resized/', document_root=settings.WG_RESIZED_PATH)
