"""
Django settings for webgallery project.

For more information on this file, see
https://docs.djangoproject.com/en/1.7/topics/settings/

For the full list of settings and their values, see
https://docs.djangoproject.com/en/1.7/ref/settings/
"""

# Build paths inside the project like this: os.path.join(BASE_DIR, ...)
import os
BASE_DIR = os.path.dirname(os.path.dirname(__file__))
PROJECT_DIR = os.path.dirname(__file__)

WG_THUMB_NAME = 'thumb'
WG_SIZE_NAMES = ['1024x768', '1920x1200', '2880x1800', '3840x2400']
WG_SIZES = {
    WG_THUMB_NAME: (99999999, 300),
    '1024x768': (1024, 768),
    '1920x1200': (1920, 1200),
    '2880x1800': (2880, 1800),
    '3840x2400': (3840, 2400),
}
WG_VIDEO_SIZE_NAMES = ['480p', '720p', '1080p']
WG_VIDEO_SIZES = {
    '480p': 480,
    '720p': 720,
    '1080p': 1080,
}

INSTALLED_APPS = [
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.sessions',
    'django.contrib.contenttypes',
    'webgallery'
]

MIDDLEWARE_CLASSES = [
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.contrib.auth.middleware.SessionAuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
]

SESSION_ENGINE = 'django.contrib.sessions.backends.cache'

WSGI_APPLICATION = 'wsgi.application'
ROOT_URLCONF = 'webgallery.urls'
TEMPLATE_DIRS = [
    os.path.join(PROJECT_DIR, 'templates'),
]

# Internationalization
# https://docs.djangoproject.com/en/1.7/topics/i18n/

LANGUAGE_CODE = 'en-us'

TIME_ZONE = 'UTC'

USE_I18N = True

USE_L10N = True

USE_TZ = True

