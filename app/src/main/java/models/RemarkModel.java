package models;

import com.google.android.gms.maps.model.LatLng;

public class RemarkModel {
    public RemarkModel(String email, String remark, LatLng latLng){
        this.email=email;
        this.remark=remark;
        this.latLng=latLng;
    }
    private String email;

    public String getEmail() {
        return email;
    }

    private String remark;

    public String getRemark() {
        return remark;
    }

    private LatLng latLng;

    public LatLng getLatLng() {
        return latLng;
    }
}
