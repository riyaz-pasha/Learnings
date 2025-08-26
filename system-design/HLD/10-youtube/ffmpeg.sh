brew install ffmpeg

ffmpeg -i /Users/riyaz/Movies/input.mp4 \
  -c:v h264 -c:a aac \
  -f hls \
  -hls_time 6 \
  -hls_playlist_type vod \
  output.m3u8


# 1) Make sure output folders exist
mkdir -p /Users/riyaz/Movies/transcoded/out_{0,1,2}

# 2) Transcode to HLS (ABR) into the transcoded/ folder
# ffmpeg -i /Users/riyaz/Movies/input.mp4 \
#   -filter:v:0 scale=w=1920:h=1080:force_original_aspect_ratio=decrease -c:a aac -ar 48000 -c:v:0 h264 -profile:v:0 high -crf 20 -sc_threshold 0 -b:v:0 5000k -maxrate:v:0 5350k -bufsize:v:0 7500k -b:a:0 192k \
#   -filter:v:1 scale=w=1280:h=720:force_original_aspect_ratio=decrease  -c:a aac -ar 48000 -c:v:1 h264 -profile:v:1 main -crf 23 -sc_threshold 0 -b:v:1 2800k -maxrate:v:1 2996k -bufsize:v:1 4200k -b:a:1 128k \
#   -filter:v:2 scale=w=854:h=480:force_original_aspect_ratio=decrease   -c:a aac -ar 48000 -c:v:2 h264 -profile:v:2 baseline -crf 28 -sc_threshold 0 -b:v:2 1400k -maxrate:v:2 1498k -bufsize:v:2 2100k -b:a:2 96k \
#   -f hls \
#   -hls_time 6 \
#   -hls_playlist_type vod \
#   -hls_segment_filename "/Users/riyaz/Movies/transcoded/out_%v/segment_%03d.ts" \
#   -master_pl_name "/Users/riyaz/Movies/transcoded/master.m3u8" \
#   -var_stream_map "v:0,a:0 v:1,a:1 v:2,a:2" \
#   "/Users/riyaz/Movies/transcoded/out_%v/prog_index.m3u8"

ffmpeg -y -i /Users/riyaz/Movies/input.mp4 -pix_fmt yuv420p \
  -filter_complex "[0:v]split=3[v720][v360][v144]; \
                   [v720]scale=-2:720[v720out]; \
                   [v360]scale=-2:360[v360out]; \
                   [v144]scale=-2:144[v144out]" \
  \
  -map "[v720out]" -map 0:a:0 -c:v:0 libx264 -profile:v:0 main     -crf 22 -g 48 -keyint_min 48 -sc_threshold 0 -b:v:0 2800k -maxrate:v:0 2996k -bufsize:v:0 4200k \
  -map "[v360out]" -map 0:a:0 -c:v:1 libx264 -profile:v:1 baseline -crf 26 -g 48 -keyint_min 48 -sc_threshold 0 -b:v:1  800k -maxrate:v:1  856k -bufsize:v:1 1200k \
  -map "[v144out]" -map 0:a:0 -c:v:2 libx264 -profile:v:2 baseline -crf 30 -g 48 -keyint_min 48 -sc_threshold 0 -b:v:2  200k -maxrate:v:2  214k -bufsize:v:2  300k \
  \
  -c:a aac -ar 48000 -ac 2 -b:a:0 128k -b:a:1 96k -b:a:2 64k \
  -f hls -hls_time 6 -hls_playlist_type vod -hls_flags independent_segments \
  -hls_segment_filename "/Users/riyaz/Movies/transcoded/out_%v/segment_%03d.ts" \
  -master_pl_name "/Users/riyaz/Movies/transcoded/master.m3u8" \
  -var_stream_map "v:0,a:0 v:1,a:1 v:2,a:2" \
  "/Users/riyaz/Movies/transcoded/out_%v/prog_index.m3u8"
