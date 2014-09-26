import os.path

__CONSTRUCTORS = {}


def add_constructor(extensions, constructor):
    for ext in extensions:
        __CONSTRUCTORS[ext.lower()] = constructor


def get_or_create(folder, basename):
    """Gets an existing or create a new media file, whose actual class depends
    on the extension of basename.

    :param folder: the folder instance
    :type folder: webgallery.models.folder.Folder
    :param basename: the basename of the media file to retrieve
    :type basename: unicode
    :return:
    """
    base, ext = os.path.splitext(basename)
    ext = ext.lower()

    if ext in __CONSTRUCTORS.keys():
        return __CONSTRUCTORS[ext](folder, basename)
    else:
        return None

