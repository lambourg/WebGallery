"""
Configuration for development mode (e.g. via manage.py)
"""

# Build paths inside the project like this: os.path.join(BASE_DIR, ...)
import os.path

from .base import *
from webgallery.settings import *

# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = 'your secret key'

# SECURITY WARNING: don't run with debug turned on in production!
DEBUG = True

TEMPLATE_DEBUG = True

ALLOWED_HOSTS = ['127.0.0.1']

# dummy authentication. NEVER EVER USE IN PRORDUCTION !!!!
# this would give everyone write access to the database

MIDDLEWARE_CLASSES.append('settings.dummyauth.DummyAuthMiddleware')
AUTHENTICATION_BACKENDS = ('webgallery.dummyauth.DummyBackend',)

# Database
# https://docs.djangoproject.com/en/1.7/ref/settings/#databases

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql_psycopg2',
        'NAME': 'django_db',
        'USER': 'django_user',
        'PASSWORD': 'django_user',
        'HOST': 'password',
        'PORT': '',
    }
}

# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/1.7/howto/static-files/

WAR_PATH = os.path.join(os.path.dirname(BASE_DIR), 'client', 'war')
STATICFILES_DIRS = (
  WAR_PATH,
  os.path.join(BASE_DIR, 'user_data'),
)
STATIC_ROOT = os.path.join(os.path.dirname(BASE_DIR), 'static')
