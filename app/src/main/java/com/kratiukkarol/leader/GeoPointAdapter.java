package com.kratiukkarol.leader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kratiukkarol.leader.model.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class GeoPointAdapter extends RecyclerView.Adapter<GeoPointAdapter.GeoPointHolder> {
    private List<GeoPoint> geoPoints = new ArrayList<>();

    @NonNull
    @Override
    public GeoPointHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.geo_points_list, parent,false);
        return new GeoPointHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GeoPointHolder holder, int position) {
        GeoPoint currentGeoPoint;
        currentGeoPoint = geoPoints.get(position);
        holder.geoPointTextView.setText(currentGeoPoint.getLatitude() + ", " + currentGeoPoint.getLongitude());
    }

    @Override
    public int getItemCount() {
        return geoPoints.size();
    }

    public void setGeoPoints(List<GeoPoint> geoPoints){
        this.geoPoints = geoPoints;
        notifyDataSetChanged();
    }

    class GeoPointHolder extends RecyclerView.ViewHolder{
        private TextView geoPointTextView;

        public GeoPointHolder(@NonNull View itemView) {
            super(itemView);
            geoPointTextView = itemView.findViewById(R.id.geo_point_text_view);
        }
    }
}
