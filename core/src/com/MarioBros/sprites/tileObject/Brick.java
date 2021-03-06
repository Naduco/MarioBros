package com.MarioBros.sprites.tileObject;

import com.MarioBros.game.MarioBros;
import com.MarioBros.scenes.Hud;
import com.MarioBros.screens.PlayScreen;
import com.MarioBros.sprites.Mario;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.maps.MapObject;

public class Brick extends InteractiveTileObject {
    public Brick(PlayScreen screen, MapObject object){
        super(screen, object);
        fixture.setUserData(this);
        setCategoryFilter(MarioBros.BRICK_BIT);
    }

    @Override
    public void onHeadHit(Mario mario) {
        if(mario.isBig()) {
            setCategoryFilter(MarioBros.DESTROYED_BIT);
            getCell().setTile(null);
            Hud.addScore(200);
            MarioBros.manager.get("audio/sounds/breakblock.wav", Sound.class).play();
        }
        MarioBros.manager.get("audio/sounds/bump.wav", Sound.class).play();
    }

}
