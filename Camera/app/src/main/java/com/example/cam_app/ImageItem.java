package com.example.cam_app;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class ImageItem implements Parcelable {
    private final Uri uri;
    private final String name;
    private final long size;
    private final long dateTaken;

    public ImageItem(Uri uri, String name, long size, long dateTaken) {
        this.uri = uri;
        this.name = name;
        this.size = size;
        this.dateTaken = dateTaken;
    }

    // Getters
    public Uri getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public long getDateTaken() {
        return dateTaken;
    }

    // --- Parcelable Implementation ---

    protected ImageItem(Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
        name = in.readString();
        size = in.readLong();
        dateTaken = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(uri, flags);
        dest.writeString(name);
        dest.writeLong(size);
        dest.writeLong(dateTaken);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ImageItem> CREATOR = new Creator<ImageItem>() {
        @Override
        public ImageItem createFromParcel(Parcel in) {
            return new ImageItem(in);
        }

        @Override
        public ImageItem[] newArray(int size) {
            return new ImageItem[size];
        }
    };
}