from itertools import chain
import multiprocessing
import os
import os.path
import subprocess
import time

from django.conf import settings
from django.db import transaction
from django.utils.encoding import smart_text

from webgallery.models.folder import Folder
from webgallery.models.picture import Resized
from webgallery.models.video import VideoResized


class PictureMonitor(object):
    def __init__(self):
        if not os.path.isdir(settings.WG_RESIZED_PATH):
            os.makedirs(settings.WG_RESIZED_PATH)

    def monitor(self):
        print "checking filesystem changes"
        with transaction.atomic():
            root = Folder.get_or_create(None, u'')
        root.do_update()
        print "checking filesystem changes done"

        remaining = True
        resizing = {}
        subps = {}
        max_running = max(multiprocessing.cpu_count() - 1, 1)

        while remaining or len(subps) > 0:
            all_remaining = list(chain(
                Resized.objects.filter(to_update=True).order_by(
                    '-size_name',
                    '-picture__folder__relative_path',
                    'picture__basename'),
                VideoResized.objects.filter(to_update=True).order_by(
                    'size_name',
                    '-video__folder__relative_path',
                    'video__basename')))

            remaining = True
            to_update = []

            if len(subps) < max_running:
                remaining = False
                for resize in all_remaining:
                    if resize in resizing.values():
                        continue

                    dest = resize.full_path

                    print u'(%d) resizing %s' % (
                        len(all_remaining) - len(subps),
                        smart_text(dest))

                    if not resize.use_multiproc:
                        subprocess.call(resize.resize_args())
                        to_update.append(resize)
                        remaining = True
                        break
                    else:
                        args = resize.resize_args()
                        print u' '.join(args)
                        subp = subprocess.Popen(args, close_fds=True)

                        subps[subp.pid] = subp
                        resizing[subp.pid] = resize

                    remaining = True

                    if len(subps) == max_running:
                        break

            for pid, subp in subps.items():
                if subp.poll() is not None:
                    resized = resizing[pid]
                    del resizing[pid]
                    del subps[pid]

                    to_update.append(resized)

            if len(subps) > 0 and len(to_update) == 0:
                time.sleep(0.5)

            if len(to_update) > 0:
                for resized in to_update:
                    resized.do_update(propagate=True, autosave=True)
