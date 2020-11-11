package xyz.liut.bingwallpaper;

import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.RequiresApi;

/**
 * 快速设置瓷砖
 * <p>
 * Create by liut on 20-11-6
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class SyncTileService extends TileService {

    private static final String TAG = "SyncTileService";

    @Override
    public void onTileAdded() {
        Log.d(TAG, "onTileAdded() called");

        Tile tile = getQsTile();
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }


    @Override
    public void onClick() {
        Log.d(TAG, "onClick() called");

        SyncWallpaperService.start(this);
    }


}
