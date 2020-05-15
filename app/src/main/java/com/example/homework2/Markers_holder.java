package com.example.homework2;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.Marker;

import java.util.List;

public class Markers_holder implements Parcelable {

    List<Marker> markerList;
    //int id;
    List<Double> save_double_latitude;
    List<Double> save_double_longtitude;


    public Markers_holder(List<Marker> listM, List<Double> lat, List<Double> longt)
    {
        this.markerList = listM;
        this.save_double_latitude = lat;
        this.save_double_longtitude = longt;
    }

   protected Markers_holder(Parcel in)
   {

   }

    public static final Creator<Markers_holder> CREATOR = new Creator<Markers_holder>() {
        @Override
        public Markers_holder createFromParcel(Parcel in) {
            return new Markers_holder(in);
        }

        @Override
        public Markers_holder[] newArray(int size) {
            return new Markers_holder[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(markerList);
        dest.writeList(save_double_latitude);
        dest.writeList(save_double_longtitude);
    }
}
