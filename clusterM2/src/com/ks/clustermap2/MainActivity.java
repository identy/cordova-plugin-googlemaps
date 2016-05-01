package com.ks.clustermap2;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import pl.mg6.android.maps.extensions.Circle;
import pl.mg6.android.maps.extensions.ClusteringSettings;
import pl.mg6.android.maps.extensions.GoogleMap;
import pl.mg6.android.maps.extensions.GoogleMap.InfoWindowAdapter;
import pl.mg6.android.maps.extensions.GoogleMap.OnInfoWindowClickListener;
import pl.mg6.android.maps.extensions.GoogleMap.OnMapClickListener;
import pl.mg6.android.maps.extensions.Marker;
import pl.mg6.android.maps.extensions.SupportMapFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity {
	private GoogleMap map;

	private MutableData[] dataArray = { new MutableData(6, new LatLng(-50, 0)), new MutableData(28, new LatLng(-52, 1)),
			new MutableData(496, new LatLng(-51, -2)), };
	private Handler handler = new Handler();
	private Runnable dataUpdater = new Runnable() {

		@Override
		public void run() {
			for (MutableData data : dataArray) {
				data.value = 7 + 3 * data.value;
			}
			onDataUpdate();
			handler.postDelayed(this, 1000);
		}
	};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        FragmentManager fm = getSupportFragmentManager();
		SupportMapFragment f = (SupportMapFragment) fm.findFragmentById(R.id.map);
		map = f.getExtendedMap();

		map.setOnMapClickListener(new OnMapClickListener() {

			@Override
			public void onMapClick(LatLng position) {
				for (Circle circle : map.getCircles()) {
					if (circle.contains(position)) {
						Toast.makeText(MainActivity.this, "Clicked " + circle.getData(), Toast.LENGTH_SHORT).show();
						return;
					}
				}
			}
		});

		map.setClustering(new ClusteringSettings().iconDataProvider(new DemoIconProvider(getResources())));

		map.setInfoWindowAdapter(new InfoWindowAdapter() {

			private TextView tv;
			{
				tv = new TextView(MainActivity.this);
				tv.setTextColor(Color.BLACK);
			}

			private Collator collator = Collator.getInstance();
			private Comparator<Marker> comparator = new Comparator<Marker>() {
				public int compare(Marker lhs, Marker rhs) {
					String leftTitle = lhs.getTitle();
					String rightTitle = rhs.getTitle();
					if (leftTitle == null && rightTitle == null) {
						return 0;
					}
					if (leftTitle == null) {
						return 1;
					}
					if (rightTitle == null) {
						return -1;
					}
					return collator.compare(leftTitle, rightTitle);
				}
			};

			@Override
			public View getInfoWindow(Marker marker) {
				return null;
			}

			@Override
			public View getInfoContents(Marker marker) {
				if (marker.isCluster()) {
					List<Marker> markers = marker.getMarkers();
					int i = 0;
					String text = "";
					while (i < 3 && markers.size() > 0) {
						Marker m = Collections.min(markers, comparator);
						String title = m.getTitle();
						if (title == null) {
							break;
						}
						text += title + "\n";
						markers.remove(m);
						i++;
					}
					if (text.length() == 0) {
						text = "Markers with mutable data";
					} else if (markers.size() > 0) {
						text += "and " + markers.size() + " more...";
					} else {
						text = text.substring(0, text.length() - 1);
					}
					tv.setText(text);
					return tv;
				} else {
					Object data = marker.getData();
					if (data instanceof MutableData) {
						MutableData mutableData = (MutableData) data;
						tv.setText("Value: " + mutableData.value);
						return tv;
					}
				}

				return null;
			}
		});

		map.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

			@Override
			public void onInfoWindowClick(Marker marker) {
				if (marker.isCluster()) {
					List<Marker> markers = marker.getMarkers();
					Builder builder = LatLngBounds.builder();
					for (Marker m : markers) {
						builder.include(m.getPosition());
					}
					LatLngBounds bounds = builder.build();
					map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, getResources().getDimensionPixelSize(R.dimen.padding)));
				}
			}
		});

		MarkerGenerator.addMarkersInPoland(map);

		BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
		for (MutableData data : dataArray) {
			Marker m = map.addMarker(new MarkerOptions().position(data.position).icon(icon));
			m.setData(data);
		}
    }

    @Override
	protected void onResume() {
		super.onResume();
		handler.post(dataUpdater);
	}

	@Override
	protected void onPause() {
		super.onPause();
		handler.removeCallbacks(dataUpdater);
	}

	private void onDataUpdate() {
		Marker m = map.getMarkerShowingInfoWindow();
		if (m != null && !m.isCluster() && m.getData() instanceof MutableData) {
			m.showInfoWindow();
		}
	}

	public void onClusterClick(View view) {
		map.setClustering(new ClusteringSettings().iconDataProvider(new DemoIconProvider(getResources())));
	}

	public void onNormalClick(View view) {
		map.setClustering(new ClusteringSettings().enabled(false));
	}

	private static class MutableData {

		private int value;

		private LatLng position;

		public MutableData(int value, LatLng position) {
			this.value = value;
			this.position = position;
		}
	}
    
}
