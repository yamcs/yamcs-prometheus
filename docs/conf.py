project = u'yamcs-prometheus'
copyright = u'2020, Space Applications Services'
author = u'Yamcs Team'

# The short X.Y version
version = u''

# The full version, including alpha/beta/rc tags
release = version

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
exclude_patterns = [u'_build', 'Thumbs.db', '.DS_Store']

# The name of the Pygments (syntax highlighting) style to use.
pygments_style = 'sphinx'

latex_documents = [
    ('index', 'yamcs-prometheus-plugin.tex', 'Yamcs Prometheus Plugin', 'Space Applications Services', 'howto'),
]

latex_show_urls = 'footnote'
