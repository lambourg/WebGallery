import re
from PIL import Image
import PIL.ExifTags

RE_DEC = re.compile(r'\(([0-9]+), *([0-9]+)\)')
RE_NUM = re.compile(r'([0-9]+)')
RE_DATE = re.compile(r'([0-9]+):([0-9]+):([0-9]+) ([0-9]+:[0-9]+:[0-9]+)')

EXIF_READ = {}


def __read(key, value):
    if key not in EXIF_READ.keys():
        raise Exception("Unexpected EXIF: %s" % key)
    key, getter = EXIF_READ[key]
    if len(str(value)) > 0:
        value = getter(str(value))
    else:
        value = ''

    return key, value


def __read_exif_string(value):
    """Reads an exif value as a simple string

    :param value: the value
    :rtype : unicode
    :return: a string
    """
    return unicode(value)


def __read_exif_float(value, normalize=False):
    """Decodes an exif number that may be a float, expressed as xx/yy

    :param value: the value
    :rtype : unicode
    :return: a string
    :raise Exception: Exception when decoding fails
    """
    match = RE_DEC.match(value)
    if match is None:
        match = RE_NUM.match(value)
        if match is None:
            raise Exception("not a float: '%s'" % value)
        else:
            return value
    x = float(match.group(1))
    y = float(match.group(2))
    if x == 0:
        return u'0'

    elif x < y:
        ret = y / x
        if normalize or abs(round(ret) - ret) < 0.001:
            return u'1/%d' % int(round(ret))
        else:
            return u'%d/%d' % (int(x), int(y))
    else:
        ret = x / y
        if round(ret) == ret:
            return unicode(int(ret))
        else:
            return unicode(ret)


def __read_exif_exposure_program(value):
    value = int(value)
    if value == 0:
        return 'Undefined'
    elif value == 1:
        return 'Manual'
    elif value == 2:
        return 'Normal program'
    elif value == 3:
        return 'Aperture priority'
    elif value == 4:
        return 'Shutter priority'
    elif value == 5:
        return 'Creative program'
    elif value == 6:
        return 'Action program'
    elif value == 7:
        return 'Portrait mode'
    elif value == 8:
        return 'Landscape mode'
    else:
        return 'Unknown'


def __read_exif_exposure_mode(value):
    value = int(value)
    if value == 0:
        return 'Auto exposure'
    if value == 1:
        return 'Manual exposure'
    if value == 2:
        return 'Auto bracket'


def __read_date(value):
    """Reads an EXIF date and returns it formatted in a more user-friendly way

    :param value: the date in EXIF format
    :rtype : unicode
    :return: the formatted string
    """
    match = RE_DATE.match(str(value))
    if match is None:
        return value
    y = match.group(1)
    m = match.group(2)
    d = match.group(3)
    h = match.group(4)

    if m == '01':
        m = 'Jan.'
    elif m == '02':
        m = 'Feb.'
    elif m == '03':
        m = 'Mar.'
    elif m == '04':
        m = 'Apr.'
    elif m == '05':
        m = 'May'
    elif m == '06':
        m = 'Jun.'
    elif m == '07':
        m = 'Jul.'
    elif m == '08':
        m = 'Aug.'
    elif m == '09':
        m = 'Sep.'
    elif m == '10':
        m = 'Oct.'
    elif m == '11':
        m = 'Nov.'
    elif m == '12':
        m = 'Dec.'

    return u'%s %s %s - %s' % (y, m, d, h)


EXIF_READ['DateTimeOriginal'] = \
    'Date', __read_date
EXIF_READ['ExposureMode'] = \
    'Exposure Mode', __read_exif_exposure_mode
EXIF_READ['ExposureProgram'] = \
    'Exposure Program', __read_exif_exposure_program
EXIF_READ['ExposureTime'] = \
    'Exposure time', lambda x: '%s s' % __read_exif_float(x, True)
EXIF_READ['FNumber'] = \
    'Aperture', lambda x: 'f/%s' % __read_exif_float(x)
EXIF_READ['FocalLength'] = \
    'Focal Length', lambda x: '%s mm' % __read_exif_float(x)
EXIF_READ['FocalLengthIn35mmFilm'] = \
    'Focal Length 35mm eq.', lambda x: '%s mm' % __read_exif_string(x)
EXIF_READ['ISOSpeedRatings'] = \
    'ISO Speed', lambda x: '%s ISO' % __read_exif_string(x)
EXIF_READ['LensModel'] = \
    'Lens', __read_exif_string
EXIF_READ['Model'] = \
    'Camera', __read_exif_string


def read_exifs(fname):
    """Reads the exif values from file located at fname

    :param fname: the path
    :type fname: string
    :rtype : dict
    :return: the ExifValues
    """
    ret = {}
    im = Image.open(fname)
    exif_data = im._getexif()
    exif = {}
    if exif_data is not None:
        exif = {
            PIL.ExifTags.TAGS[k]: v
            for k, v in exif_data.items()
            if k in PIL.ExifTags.TAGS
        }

    for key in exif.keys():
        value = exif[key]
        if key in EXIF_READ.keys():
            key, value = __read(key, value)
            ret[key] = value
    return ret
