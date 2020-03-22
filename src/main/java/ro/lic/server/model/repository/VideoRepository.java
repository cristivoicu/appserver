package ro.lic.server.model.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ro.lic.server.model.dao.VideoDao;
import ro.lic.server.model.tables.Video;

@Component
public class VideoRepository {
    @Autowired
    private VideoDao videoDao;

    public void addVideo(Video video){
        videoDao.save(video);
    }


}
