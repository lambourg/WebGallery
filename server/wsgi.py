"""
WSGI config for webgallery project.

It exposes the WSGI callable as a module-level variable named ``application``.

For more information on this file, see
https://docs.djangoproject.com/en/1.7/howto/deployment/wsgi/
"""

import os
import os.path
import sys

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "settings.wsgi")
root = os.path.realpath(os.path.dirname(__file__))
sys.path.append(root)

from django.core.wsgi import get_wsgi_application
application = get_wsgi_application()
