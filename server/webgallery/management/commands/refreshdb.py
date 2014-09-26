from django.core.management.base import BaseCommand, CommandError
from webgallery.monitor import PictureMonitor

class Command(BaseCommand):
    args = ''
    help = 'Analyses the hard drive to check for new or deleted pictures'

    def handle(self, *args, **options):
        monitor = PictureMonitor()
        monitor.monitor()