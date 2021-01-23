package me.wcy.music.loader;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.webkit.ValueCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.wcy.music.model.Music;
import me.wcy.music.storage.preference.Preferences;
import me.wcy.music.utils.CoverLoader;
import me.wcy.music.utils.ParseUtils;
import me.wcy.music.utils.SystemUtils;

public class MusicLoaderCallback implements LoaderManager.LoaderCallbacks {
    private final Context context;
    private final ValueCallback<Map<String, List<Music>>> callback;

    private final Map<String, List<Music>> musicMap; //分文件夹存储音乐列表

    public MusicLoaderCallback(Context context, ValueCallback<Map<String, List<Music>>> callback) {
        this.context = context;
        this.callback = callback;
        this.musicMap = new HashMap<>();
    }

    public Loader onCreateLoader(int id, Bundle args) {
        return new MusicCursorLoader(context);
    }

    public void onLoadFinished(Loader var1, Object var2) {
        this.onLoadFinished(var1, (Cursor) var2);
    }

    public void onLoaderReset(Loader loader) {
    }

    public void onLoadFinished(Loader loader, Cursor data) {
        if (data == null) {
            return;
        }

        long filterTime = ParseUtils.parseLong(Preferences.getFilterTime()) * 1000;
        long filterSize = ParseUtils.parseLong(Preferences.getFilterSize()) * 1024;

        //int counter = 0;
        musicMap.clear();
        while (data.moveToNext()) {
            // 是否为音乐，魅族手机上始终为0
            int isMusic = data.getInt(data.getColumnIndex(MediaStore.Audio.AudioColumns.IS_MUSIC));
            if (!SystemUtils.isFlyme() && isMusic == 0) {
                continue;
            }
            long duration = data.getLong(data.getColumnIndex(MediaStore.Audio.Media.DURATION));
            if (duration < filterTime) {
                continue;
            }
            long fileSize = data.getLong(data.getColumnIndex(MediaStore.Audio.Media.SIZE));
            if (fileSize < filterSize) {
                continue;
            }

            long id = data.getLong(data.getColumnIndex(BaseColumns._ID));
            String title = data.getString(data.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE));
            String artist = data.getString(data.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST));
            String album = data.getString(data.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM));
            long albumId = data.getLong(data.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID));
            String path = data.getString(data.getColumnIndex(MediaStore.Audio.AudioColumns.DATA));
            String fileName = data.getString(data.getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME));

            Music music = new Music();
            music.setSongId(id);
            music.setType(Music.Type.LOCAL);
            music.setTitle(title);
            music.setArtist(artist);
            music.setAlbum(album);
            music.setAlbumId(albumId);
            music.setDuration(duration);
            music.setPath(path);
            music.setFileName(fileName);
            music.setFileSize(fileSize);
//            if (++counter <= 20) {
//                // 只加载前20首的缩略图
//                CoverLoader.get().loadThumb(music);
//            }

            String folderPath = path.replace("/" + fileName, "");
            if (musicMap.containsKey(folderPath)){
                musicMap.get(folderPath).add(music);
            }else {
                List<Music> list = new ArrayList<>();
                list.add(music);
                musicMap.put(folderPath, list);
            }
        }

        //文件按名称 - 排序
        for (String path : musicMap.keySet()){
            List<Music> list = musicMap.get(path);
            Collections.sort(list, new Comparator<Music>() {
                @Override
                public int compare(Music o1, Music o2) {
                    return o1.getPath().compareTo(o2.getPath());
                }
            });
        }


        callback.onReceiveValue(musicMap);
    }
}
