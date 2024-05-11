package aiss.youtubeMiner.service;

import aiss.videoMiner.model.*;
import aiss.youtubeMiner.model.caption.CaptionSearch;
import aiss.youtubeMiner.model.caption.CaptionSnippet;
import aiss.youtubeMiner.model.channel.ChannelSearch;
import aiss.youtubeMiner.model.channel.ChannelSnippet;
import aiss.youtubeMiner.model.comment.CommentSearch;
import aiss.youtubeMiner.model.comment.CommentSnippet__1;
import aiss.youtubeMiner.model.videoSnippet.VideoSnippet;
import aiss.youtubeMiner.model.videoSnippet.VideoSnippetDetails;
import aiss.youtubeMiner.model.videoSnippet.VideoSnippetSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class YoutubeService implements YoutubeServiceInterface{
    @Autowired
    RestTemplate restTemplate;

    public Channel findChannelById(String id, String apiKey) {
        String uri = "https://www.googleapis.com/youtube/v3/channels?part=snippet&id=" + id + "&key=" + apiKey;
        ResponseEntity<ChannelSearch> response = restTemplate.getForEntity(uri, ChannelSearch.class);
        aiss.youtubeMiner.model.channel.Channel channel = response.getBody().getItems().get(0);
        ChannelSnippet snippet = channel.getSnippet();

        Channel newChannel = new Channel();
        newChannel.setId(channel.getId());
        newChannel.setName(snippet.getTitle());
        newChannel.setDescription(snippet.getDescription());
        newChannel.setCreatedTime(snippet.getPublishedAt());
        newChannel.setVideos(findVideosByChannelId(id, apiKey));

        return newChannel;
    }

    public List<Video> findVideosByChannelId(String channelId, String apiKey) {
        // search?part=snippet&channelId=UCByOQJjav0CUDwxCk-jVNRQ&type=video&key=[YOUR_API_KEY]
        String uri = "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=" + channelId +"&type=video&key=" + apiKey;
        VideoSnippetSearch response = restTemplate.getForObject(uri, VideoSnippetSearch.class);
        List<VideoSnippet> videos = response.getItems();
        List<Video> res = new ArrayList<>();
        for(VideoSnippet vs:videos){
            res.add(parseVideo(vs,apiKey));
        }
        return res;
    }

    public Video parseVideo(VideoSnippet videoSnippet,String key){
        VideoSnippetDetails snippet = videoSnippet.getSnippet();
        Video res = new Video();
        String videoId = videoSnippet.getId().getVideoId();
        System.out.println(videoId);
        res.setId(videoId);
        res.setName(snippet.getTitle());
        res.setDescription(snippet.getDescription());
        res.setReleaseTime(snippet.getPublishedAt());
        res.setCaptions(findCaptionsByVideoId(videoId,key));
        res.setComments(findCommentsByVideoId(videoId,key));
        return res;
    }

    public List<Caption> findCaptionsByVideoId(String videoId, String apiKey) {
        String uri = "https://www.googleapis.com/youtube/v3/captions?part=snippet&videoId=" + videoId + "&key=" + apiKey;
        ResponseEntity<CaptionSearch> response = restTemplate.getForEntity(uri, CaptionSearch.class);
        List<Caption> captions = new ArrayList<>();
        if(response.getStatusCode()== HttpStatusCode.valueOf(200)){
            List<aiss.youtubeMiner.model.caption.Caption> items = response.getBody().getItems();

            for (int i = 0; i < items.size(); i++) {
                aiss.youtubeMiner.model.caption.Caption caption = items.get(i);
                CaptionSnippet snippet = caption.getSnippet();

                Caption result = new Caption();
                result.setId(caption.getId());
                result.setName(snippet.getName());
                result.setLanguage(snippet.getLanguage());
                captions.add(result);
            }
        }
        return captions;
    }

    public List<Comment> findCommentsByVideoId(String videoId, String apiKey) {
        String uri = "https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&videoId=" + videoId + "&key=" + apiKey;
        // Example_videoId=ZkXG3ZrXlbc
        List<Comment> comments = new ArrayList<Comment>();
        try{
            ResponseEntity<CommentSearch> response = restTemplate.getForEntity(uri, CommentSearch.class);
            if(response.getStatusCode()==HttpStatusCode.valueOf(200)){
                List<aiss.youtubeMiner.model.comment.Comment> items = response.getBody().getItems();

                for (int i = 0; i < items.size(); i++) {
                    aiss.youtubeMiner.model.comment.TopLevelComment comment = items.get(i).getCommentSnippet().getTopLevelComment();
                    CommentSnippet__1 snippet = comment.getSnippet();

                    User user = new User();
                    user.setId(null);
                    user.setUser_link(snippet.getAuthorChannelUrl());
                    user.setName(snippet.getAuthorDisplayName());
                    user.setPicture_link(snippet.getAuthorProfileImageUrl());
                    Comment result = new Comment();
                    result.setId(comment.getId());
                    result.setText(snippet.getTextOriginal());
                    result.setCreatedOn(snippet.getPublishedAt());
                    result.setAuthor(user);

                    comments.add(result);
                }
            }
            return comments;
        }catch (HttpClientErrorException e){
            if(e.getStatusCode()==HttpStatusCode.valueOf(403)){
                // For videos with comments disabled we return an empty list
                return new ArrayList<>();
            }
        }
        return comments;
    }

}
