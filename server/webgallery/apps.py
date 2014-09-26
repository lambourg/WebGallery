import codecs
import sys

from django.apps import AppConfig

class WebGallery(AppConfig):
    name = "webgallery"
    verbose_name = "Web Gallery"

    def ready(self):
        sys.stdout = codecs.getwriter('utf-8')(sys.stdout)
