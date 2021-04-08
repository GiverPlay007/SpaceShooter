package me.giverplay.spaceshooter;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Game extends Canvas implements Runnable, KeyListener
{
	private static final long serialVersionUID = 1L;
	
	private static final int WIDTH = 160;
	private static final int HEIGHT = 220;
	private static final int SCALE = 3;
	private static final int TSIZE = 16;
	
	private final ArrayList<Entity> entities = new ArrayList<>();
	private final Random rand = new Random();
	
	private BufferedImage image;
	private BufferedImage spritesheet;
	private BufferedImage back;
	private Spawner spawner;
	private Player player;
	
	private boolean isRunning = false;
	private boolean gameOver = false;
	private boolean showGameOver = false;
	private boolean jaMorreu = false;
	
	private int score = 0;
	private int chances = 10;
	private int maxScore = 0;
	private int gameOverFrames = 0;
	
	private double by1 = 0;
	private double by2 = -HEIGHT;
	
	public static void main(String[] args)
	{
		new Game();
	}
	
	public Game()
	{
		setupWindow();
		setupAssets();
		
		addKeyListener(this);
		
		Sound.fix();
		start();
	}
	
	public void setupWindow()
	{
		this.setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		JFrame frame = new JFrame("Game 07 - Space Shooter");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.add(this);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	public void setupAssets()
	{
		image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		
		try
		{
			spritesheet = ImageIO.read(getClass().getResource("/Spritesheet.png"));
			back = ImageIO.read(getClass().getResource("/Back.png"));
		} 
		catch (IOException e)
		{
			System.out.println(e.getMessage());
		}
		
		player = new Player(WIDTH / 2 - TSIZE / 2, HEIGHT - (TSIZE * 2 + 3));
		spawner = new Spawner();
	}
	
	public void reset()
	{
		entities.clear();
		score = 0;
		chances = 10;
		gameOver = false;
		showGameOver = false;
		gameOverFrames = 0;
		jaMorreu = false;
		player = new Player(WIDTH / 2 - TSIZE / 2, HEIGHT - (TSIZE * 2 + 3));
		entities.add(player);
	}
	
	public synchronized void start()
	{
		isRunning = true;
		Thread thread = new Thread(this, "Main Thread");
		thread.start();
	}
	
	public BufferedImage getSprite(int x, int y, int w, int h)
	{
		return spritesheet.getSubimage(x, y, w, h);
	}
	
	@Override
	public void run()
	{
		requestFocus();
		
		long lastTime = System.nanoTime();
		long now;
		
		double updates = 60.0D;
		double delta = 0.0D;
		double update = 1000000000 / updates;
		
		while (isRunning)
		{
			now = System.nanoTime();
			delta += (now - lastTime) / update;
			lastTime = now;
			
			if (delta >= 1)
			{
				update();
				render();
				
				delta--;
			}
		}
	}
	
	private void update()
	{
		if (gameOver)
		{
			return;
		}
		
		for (int i = 0; i < entities.size(); i++)
		{
			entities.get(i).tick();
		}
		
		spawner.tick();
		
		if (score > maxScore)
		{
			maxScore = score;
		}
		
		if (chances <= 0 && !jaMorreu)
		{
			jaMorreu = true;
			matar();
		}
	}
	
	public void matar()
	{
		new Explosion(player.getX(), player.getY(), Sound.explosion);
		entities.remove(player);
		
		new Timer().schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				gameOver = true;
			}
		}, 1000);
	}
	
	private void render()
	{
		BufferStrategy bs = getBufferStrategy();
		
		if (bs == null)
		{
			createBufferStrategy(3);
			return;
		}
		
		Graphics smooth = bs.getDrawGraphics();
		Graphics g = image.getGraphics();
		
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, WIDTH * SCALE, HEIGHT * SCALE);
		
		advanceBackground(g);
		
		for(Entity entity : entities)
		{
			entity.render(g);
		}
		
		smooth.drawImage(image, 0, 0, WIDTH * SCALE, HEIGHT * SCALE, null);
		
		renderUI(smooth);
		
		bs.show();
	}
	
	public void advanceBackground(Graphics g)
	{
		if(!gameOver)
		{
			by1 += 0.5;
			by2 += 0.5;
		}
		
		if (by1 >= HEIGHT)
		{
			by1 = -HEIGHT;
		}
		
		if (by2 >= HEIGHT)
		{
			by2 = -HEIGHT;
		}
		
		g.drawImage(back, 0, (int) by1, null);
		g.drawImage(back, 0, (int) by2, null);
	}
	
	public void renderUI(Graphics g)
	{
		g.setColor(Color.white);
		g.setFont(new Font("calibri", Font.BOLD, 22));
		
		String txt = "Record: " + maxScore;
		g.drawString(txt, WIDTH * SCALE - (g.getFontMetrics().stringWidth(txt)) - 10, 18);
		g.drawString("Score: " + score, 5, 18);
		
		for(int i = 0; i < 10; i++)
		{
			g.setColor(Color.WHITE);
			g.drawRect(5 + i * 47, HEIGHT * SCALE - 21, 46, 15);
			
			if(i < chances)
			{
				g.setColor(Color.GRAY);
				g.fillRect(6 + i * 47, HEIGHT * SCALE - 20, 45, 14);
			}
		}
		
		if(gameOver)
		{
			gameOverFrames++;
			
			if(gameOverFrames >= 30)
			{
				gameOverFrames = 0;
				showGameOver = !showGameOver;
			}
			
			if(showGameOver) 
			{
				g.setColor(Color.WHITE);
				
				String txt1 = "Game Over";
				g.setFont(new Font("calibri", Font.BOLD, 32));
				g.drawString(txt1, screenMiddle(g, txt), HEIGHT * SCALE / 2 - 50);
				
				String txt2 = "Aperte ENTER para reiniciar";
				g.setFont(new Font("arial", Font.BOLD, 24));
				g.drawString(txt2,  screenMiddle(g, txt2), HEIGHT * SCALE / 2);
			}
		}
	}
	
	private int screenMiddle(Graphics g, String txt)
	{
		return (WIDTH * SCALE - g.getFontMetrics().stringWidth(txt)) / 2;
	}
	
	@Override
	public void keyPressed(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_D:
			case KeyEvent.VK_RIGHT:
				
				player.direita(true);
				
				break;
				
			case KeyEvent.VK_A:
			case KeyEvent.VK_LEFT:
				
				player.esquerda(true);
				
				break;
				
			case KeyEvent.VK_SPACE:
			case KeyEvent.VK_F:
			case KeyEvent.VK_X:
			case KeyEvent.VK_CONTROL:
			case KeyEvent.VK_SHIFT:
				
				player.shoot(true);
				
				break;
				
			case KeyEvent.VK_ENTER:
				
				if(gameOver)
					reset();
				
				break;
				
			default:
				break;
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_D:
			case KeyEvent.VK_RIGHT:
				
				player.direita(false);
				break;
				
			case KeyEvent.VK_A:
			case KeyEvent.VK_LEFT:
				
				player.esquerda(false);
				break;
			
			case KeyEvent.VK_SPACE:
			case KeyEvent.VK_F:
			case KeyEvent.VK_X:
			case KeyEvent.VK_CONTROL:
			case KeyEvent.VK_SHIFT:
				
				player.shoot(false);
				break;
				
			default:
				break;
		}
	}
	
	@Override
	public void keyTyped(KeyEvent arg0) {	}
	
	public abstract class Entity
	{
		private final BufferedImage sprite;
		
		private final int w;
		private final int h;
		
		private int x;
		private int y;
		private int life;
		
		public Entity(int x, int y, int w, int h, int life, BufferedImage sprite)
		{
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.sprite = sprite;
			this.life = life;
			entities.add(this);
		}
		
		public int getX()
		{
			return this.x;
		}
		
		public int getY()
		{
			return this.y;
		}
		
		public int getLife()
		{
			return life;
		}
		
		public void hit(int hit)
		{
			this.life += hit;
		}
		
		public void moveX(int move)
		{
			int xn = x + move;
			
			if (!(xn <= 0 || xn >= WIDTH - TSIZE))
			{
				x = xn;
			}
		}
		
		public void moveY(int move)
		{
			this.y += move;
		}
		
		public int getWid()
		{
			return w;
		}
		
		public int getHei()
		{
			return h;
		}
		
		public void destroy()
		{
			entities.remove(this);
		}
		
		public abstract void tick();
		
		public boolean isColliding(Entity e1)
		{
			Rectangle rec = new Rectangle(getX(), getY(), getWid(), getHei());
			Rectangle rec2 = new Rectangle(e1.getX(), e1.getY(), e1.getWid(), e1.getHei());
			
			return rec.intersects(rec2);
		}
		
		public void render(Graphics g)
		{
			g.drawImage(sprite, getX(), getY(), TSIZE, TSIZE, null);
		}
	}
	
	public class Rock extends Entity
	{
		public Rock(int x, int y)
		{
			super(x, y, TSIZE, TSIZE, 3, getSprite(0, TSIZE, TSIZE, TSIZE));
		}
		
		@Override
		public void tick()
		{
			moveY(1);
			
			if (getY() > HEIGHT)
				sumir();
			
			for(int i = 0; i < entities.size(); i++)
			{
				Entity e = entities.get(i);
				
				if(e instanceof Laser)
				{
					if(isColliding(e))
					{
						this.hit(-1);
						e.destroy();
					}
				} else if(e == player)
				{
					if(isColliding(e))
					{
						chances -= 2;
						destroy();
					}
				}
			}
			
			if (this.getLife() <= 0)
			{
				score++;
				this.destroy();
			}
		}
		
		public void sumir()
		{
			super.destroy();
			chances--;
		}
		
		@Override
		public void destroy()
		{
			new Explosion(getX(), getY(), Sound.explosionRock);
			super.destroy();
		}
	}
	
	public class Player extends Entity
	{
		private boolean right = false;
		private boolean left = false;
		private boolean shooted = false;
		private boolean shootLocked = false;
		
		private int shootFrames = 0;
		
		public Player(int x, int y)
		{
			super(x, y, TSIZE, TSIZE, 0, getSprite(0, 0, TSIZE, TSIZE));
		}
		
		@Override
		public void tick()
		{
			int m = 0;
			
			if (right)
				m++;
			
			if (left)
				m--;
			
			moveX(m * 2);
			
			if(shootLocked)
			{
				shootFrames++;
				
				if(shootFrames >= 15)
				{
					shootFrames = 0;
					shootLocked = false;
				}
				
				return;
			}
			
			if (shooted)
			{
				shootLocked = true;
				new Laser(super.getX() + TSIZE / 2 - 1, super.getY() + 3);
			}
		}
		
		public void shoot(boolean shooted)
		{
			this.shooted = shooted;
		}
		
		public void direita(boolean b)
		{
			right = b;
		}
		
		public void esquerda(boolean b)
		{
			left = b;
		}
	}
	
	public class Laser extends Entity
	{
		public Laser(int x, int y)
		{
			super(x, y, 2, 3, 1, null);
			Sound.laser.play();
		}
		
		@Override
		public void tick()
		{
			moveY(-6);
			
			if (super.getY() <= -TSIZE)
			{
				destroy();
			}
		}
		
		@Override
		public void render(Graphics g)
		{
			g.setColor(Color.RED);
			g.fillRect(super.getX(), super.getY(), getWid(), getHei());
		}
	}
	
	public class Explosion extends Entity
	{
		private final BufferedImage[] sprites = new BufferedImage[4];
		
		private int eframes = 0;
		private int anim = 0;
		
		public Explosion(int x, int y, Sound sound)
		{
			super(x, y, TSIZE, TSIZE, 0, null);
			
			sound.play();
			
			for (int i = 0; i < 64; i += TSIZE)
			{
				sprites[i / TSIZE] = getSprite(i, TSIZE * 2, TSIZE, TSIZE);
			}
		}
		
		@Override
		public void tick()
		{
			eframes++;
			
			if (eframes >= 5)
			{
				eframes = 0;
				anim++;
				
				if (anim >= sprites.length)
				{
					destroy();
				}
			}
		}
		
		@Override
		public void render(Graphics g)
		{
			g.drawImage(sprites[anim], getX(), getY(), null);
		}
	}
	
	public class Spawner
	{
		private int frame = 0;
		
		public void tick()
		{
			frame++;
			
			if (frame >= 100)
			{
				frame = 0;
				
				new Rock(rand.nextInt(WIDTH - TSIZE), 0);
			}
		}
	}
	
	public static class Sound
	{
		public static final Sound explosion = new Sound("/explosion.wav");
		public static final Sound explosionRock = new Sound("/explosionRock.wav");
		public static final Sound laser = new Sound("/laser.wav");
		
		private AudioClip clip;
		
		public static void fix() { } // Ok, é uma forma bem preguiçosa de se arrumar
		
		private Sound(String name)
		{
			try
			{
				clip = Applet.newAudioClip(Sound.class.getResource(name));
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
		
		public void play()
		{
			try
			{
				new Thread(() -> clip.play()).start();
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
}
