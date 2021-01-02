FROM python:3.7.5
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg
COPY ./requirements.txt /app/requirements.txt
WORKDIR /app
RUN pip3 install -r requirements.txt
COPY app/ /app
ENTRYPOINT ["/bin/bash", "./entrypoint.sh"]
EXPOSE 5000
