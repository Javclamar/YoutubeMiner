package aiss.youtubeMiner.service;

import aiss.videoMiner.model.Caption;
import aiss.videoMiner.model.Channel;
import aiss.videoMiner.model.Comment;
import aiss.videoMiner.model.Video;

import java.util.List;

public interface YoutubeServiceInterface {
    public Channel findChannelById(String id, String apiKey);
    public List<Video> findVideosByChannelId(String channelId, String apiKey);
    public List<Caption> findCaptionsByVideoId(String videoId, String apiKey);
    public List<Comment> findCommentsByVideoId(String videoId, String apiKey);
}
