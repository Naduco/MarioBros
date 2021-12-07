package com.MarioBros.screens;

import java.util.concurrent.LinkedBlockingQueue;

import com.MarioBros.Utilidades.B2WorldCreator;
import com.MarioBros.Utilidades.WorldContactListener;
import com.MarioBros.game.MarioBros;
import com.MarioBros.scenes.Hud;
import com.MarioBros.sprites.Mario;
import com.MarioBros.sprites.enemies.Enemy;
import com.MarioBros.sprites.items.Item;
import com.MarioBros.sprites.items.ItemDef;
import com.MarioBros.sprites.items.Mushroom;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class PlayScreen implements Screen {
	
	private MarioBros game;
	private TextureAtlas atlas;
	public static boolean alreadyDestroyed = false;

	private OrthographicCamera gamecam;
	private Viewport gamePort;
	private Hud hud;

	private TmxMapLoader maploader;
	private TiledMap map;
	private OrthogonalTiledMapRenderer renderer;

	private World world;
	// Dibuja el contorno de todas las cajas de colision
	private Box2DDebugRenderer b2dr;
	private B2WorldCreator creator;

	private Mario player;

	private Music music;

	private Array<Item> items;
	private LinkedBlockingQueue<ItemDef> itemsToSpawn;

	public PlayScreen(MarioBros game) {
		
		atlas = new TextureAtlas("Mario_and_Enemies.pack");

		this.game = game;

		gamecam = new OrthographicCamera();

		gamePort = new FitViewport(MarioBros.V_WIDTH / MarioBros.PPM, MarioBros.V_HEIGHT / MarioBros.PPM, gamecam);

		hud = new Hud(game.batch);

		maploader = new TmxMapLoader();
		map = maploader.load("level1.tmx");
		renderer = new OrthogonalTiledMapRenderer(map, 1 / MarioBros.PPM);

		gamecam.position.set(gamePort.getWorldWidth() / 2, gamePort.getWorldHeight() / 2, 0);

		world = new World(new Vector2(0, -10), true);

		b2dr = new Box2DDebugRenderer();

		creator = new B2WorldCreator(this);

		player = new Mario(this);

		world.setContactListener(new WorldContactListener());

		music = MarioBros.manager.get("audio/music/mario_music.ogg", Music.class);
		music.setLooping(true);
		music.setVolume(0.3f);
		music.play();

		items = new Array<Item>();
		itemsToSpawn = new LinkedBlockingQueue<ItemDef>();
	}

	public void spawnItem(ItemDef idef) {
		itemsToSpawn.add(idef);
	}

	public void handleSpawningItems() {
		if (!itemsToSpawn.isEmpty()) {
			ItemDef idef = itemsToSpawn.poll();
			if (idef.type == Mushroom.class) {
				items.add(new Mushroom(this, idef.position.x, idef.position.y));
			}
		}
	}

	public TextureAtlas getAtlas() {
		return atlas;
	}

	@Override
	public void show() {

	}

	public void handleInput(float dt) {
		if (player.currentState != Mario.State.DEAD) {
			if (Gdx.input.isKeyJustPressed(Input.Keys.UP))
				player.jump();
			if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) && player.b2body.getLinearVelocity().x <= 2)
				player.b2body.applyLinearImpulse(new Vector2(0.1f, 0), player.b2body.getWorldCenter(), true);
			if (Gdx.input.isKeyPressed(Input.Keys.LEFT) && player.b2body.getLinearVelocity().x >= -2)
				player.b2body.applyLinearImpulse(new Vector2(-0.1f, 0), player.b2body.getWorldCenter(), true);
//            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
//                player.fire();
		}

	}

	public void update(float dt) {
		handleInput(dt);
		handleSpawningItems();

		world.step(1 / 60f, 6, 2);

		player.update(dt);
		for (Enemy enemy : creator.getEnemies()) {
			enemy.update(dt);
			if (enemy.getX() < player.getX() + 224 / MarioBros.PPM) {
				enemy.b2body.setActive(true);
			}
		}

		for (Item item : items)
			item.update(dt);

		hud.update(dt);

		if (player.getY() < 0) {
			player.currentState = Mario.State.DEAD;
		}
		
		if (player.getX() > 32.88f) {
			player.llegoSalida();
		}
		
		if (player.currentState != Mario.State.DEAD) {
			gamecam.position.x = player.b2body.getPosition().x;
		}

		gamecam.update();

		renderer.setView(gamecam);

	}

	@Override
	public void render(float delta) {

		update(delta);

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		renderer.render();

		b2dr.render(world, gamecam.combined);

		game.batch.setProjectionMatrix(gamecam.combined);
		game.batch.begin();
		player.draw(game.batch);
		for (Enemy enemy : creator.getEnemies())
			enemy.draw(game.batch);
		for (Item item : items)
			item.draw(game.batch);
		game.batch.end();

		game.batch.setProjectionMatrix(hud.stage.getCamera().combined);
		hud.stage.draw();

		if (gameOver()) {
			game.setScreen(new GameOverScreen(game));
			dispose();
		}
		
		if(player.isPuedeSalir()) {
			game.setScreen(new EndScreen(game));
			dispose();
		}

	}

	public boolean gameOver() {
		if (player.currentState == Mario.State.DEAD && player.getStateTimer() > 3) {
			return true;
		}
		return false;
	}

	@Override
	public void resize(int width, int height) {
		gamePort.update(width, height);

	}

	public TiledMap getMap() {
		return map;
	}

	public World getWorld() {
		return world;
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void hide() {

	}

	@Override
	public void dispose() {
		map.dispose();
		renderer.dispose();
		world.dispose();
		b2dr.dispose();
		hud.dispose();
	}

	public Hud getHud() {
		return hud;
	}
}