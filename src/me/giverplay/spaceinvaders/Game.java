package me.giverplay.spaceinvaders;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Game extends Canvas implements Runnable, KeyListener
{
	private static final long serialVersionUID = 1L;
	
	private static final int WIDTH = 160;
	private static final int HEIGHT = 240;
	private static final int SCALE = 3;
	private static final int SIZE = 16;
	
	private ArrayList<Entity> entities = new ArrayList<>();
	
	private Random rand = new Random();
	private BufferedImage image;
	private BufferedImage spritesheet;
	private BufferedImage back;
	private Spawner spawner;
	private Player player;
	private Thread thread;
	private JFrame frame;
	
	private boolean isRunning = false;
	
	public static void main(String[] args)
	{
		new Game();
	}
	
	public Game()
	{
		setupWindow();
		setupAssets();
		
		addKeyListener(this);
		
		start();
	}
	
	public void setupWindow()
	{
		frame = new JFrame("Game 07 - Space Invaders Clone");
		frame.setResizable(false);
		frame.setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.add(this);
		frame.pack();
		frame.setVisible(true);
	}
	
	public void setupAssets()
	{
		image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		
		try
		{
			spritesheet = ImageIO.read(getClass().getResourceAsStream("/Spritesheet.png"));
		}
		catch(IOException e)
		{
			System.out.println(e.getMessage());
		}
		
		player = new Player(WIDTH / 2 - SIZE / 2, HEIGHT - SIZE * 2);
		spawner = new Spawner();
	}
	
	public void reset()
	{
		entities.clear();
	}
	
	public synchronized void start()
	{
		isRunning = true;
		thread = new Thread(this, "Main Thread");
		thread.start();
	}
	
	public synchronized void stop()
	{
		isRunning = false;
		
		try
		{
			thread.join();
		}
		catch(InterruptedException e)
		{
			System.out.println("Interrupted");
		}
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
		
		while(isRunning)
		{
			now = System.nanoTime();
			delta += (now - lastTime) / update;
			lastTime = now;
			
			if(delta >= 1)
			{
				update();
				render();
				
				delta--;
			}
		}
	}
	
	private void update()
	{
		for(int i = 0; i < entities.size(); i++)
		{
			entities.get(i).tick();
		}
		
		spawner.tick();
	}
	
	private void render()
	{
		BufferStrategy bs = getBufferStrategy();
		
		if(bs == null)
		{
			createBufferStrategy(3);
			return;
		}
		
		Graphics smooth = bs.getDrawGraphics();
		Graphics g = image.getGraphics();
		
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, WIDTH * SCALE, HEIGHT * SCALE);
		
		for(int i = 0; i < entities.size(); i++)
		{
			entities.get(i).render(g);
		}
		
		smooth.drawImage(image, 0, 0, WIDTH * SCALE, HEIGHT * SCALE, null);
		
		bs.show();
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

				break;
				
			default:
				break;
		}
	}
	
	@Override
	public void keyTyped(KeyEvent arg0)
	{
		
	}
	
	public abstract class Entity
	{
		private BufferedImage sprite;
		private int x;
		private int y;
		
		public Entity(int x, int y, BufferedImage sprite)
		{
			this.x = x;
			this.y = y;
			this.sprite = sprite;
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
		
		public void setX(int x)
		{
			this.x = x;
		}
		
		public void setY(int y)
		{
			this.y = y;
		}
		
		public void moveX(int move)
		{
			this.x += move;
		}
		
		public void moveY(int move)
		{
			this.y += move;
		}
		
		public void destroy()
		{
			entities.remove(this);
		}
		
		public void tick()
		{
			
		}
		
		public void render(Graphics g)
		{
			g.drawImage(sprite, getX(), getY(), SIZE, SIZE, null);
		}
	}
	
	public class Rock extends Entity
	{
		public Rock(int x, int y)
		{
			super(x, y, getSprite(0, SIZE, SIZE, SIZE));
		}
		
		@Override
		public void tick()
		{
			moveY(1);
			
			if(getY() > HEIGHT)
				destroy();
		}
	}
	
	public class Player extends Entity
	{
		private boolean right = false;
		private boolean left = false;
		
		public Player(int x, int y)
		{
			super(x, y, getSprite(0, 0, SIZE, SIZE));
		}
		
		@Override
		public void tick()
		{
			int m = 0;
			
			if(right)
				m++;
			
			if(left)
				m--;
				
			moveX(m * 2);
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
	
	public class Spawner
	{
		private int frame = 0;
		private int maxF = 100;
		
		public void tick()
		{
			frame++;
			
			if(frame >= maxF)
			{
				frame = 0;
				
				new Rock(rand.nextInt(WIDTH - SIZE), 0);
			}
		}
	}
}
