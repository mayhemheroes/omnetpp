# in your Dockerfile
FROM docker.io/sphinxdoc/sphinx-latexpdf:8.2.3

ADD requirements.txt /root/requirements.txt
RUN export DEBIAN_FRONTEND=noninteractive \
  && apt-get update && apt-get install --yes --no-install-recommends \
    doxygen graphviz wget curl rsync git \
    gcc zlib1g-dev libxml2-dev libxslt-dev && \
    pip3 install --upgrade pip setuptools && \
    pip3 install -r /root/requirements.txt --ignore-requires-python && \
    apt-get autoremove --yes gcc zlib1g-dev libxml2-dev libxslt-dev && \
    apt-get clean && \
    rm -r /root/.cache
