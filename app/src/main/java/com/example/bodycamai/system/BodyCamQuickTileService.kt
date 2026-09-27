package com.example.bodycamai.system

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.bodycamai.MainActivity

class BodyCamQuickTileService : TileService() {
    override fun onClick() {
        super.onClick()
        qsTile?.apply { state = Tile.STATE_ACTIVE; updateTile() }
        startActivityAndCollapse(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
