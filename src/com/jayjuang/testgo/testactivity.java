package com.jayjuang.testgo;

import android.app.*;
import android.content.*;
import android.os.*;
import android.R.*;
import android.view.*;
import android.graphics.*;
import java.util.*;

public class testactivity extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
		goview goview = new goview(this);
        setContentView(goview);
    }
	
	public class goview extends View
	{
		public viewarbiter _vwabtr;
		public goview(Context context)
		{
			super(context);
			_vwabtr=new viewarbiter(5,this,new aiplayer(1), new viewplayer());
			_vwabtr.start();
		}
		protected void onDraw(Canvas canvas)
		{
			_vwabtr._vbrd.draw(canvas);
		}
		public boolean onTouchEvent(MotionEvent event)
		{
			for(viewplayer plr : _vwabtr._plrs) plr.event(event);
			return true;
		}
	}
	
	
	public class aiplayer extends viewplayer
	{
		Object _locker=new Object();
		int _color;
		play _ply;
		int _move;
		Boolean ready=false;
		@Override
		public void in(string input)
		{
			new Thread(new Runnable(){public void run(){process();}}).start();
		}
		@Override
		public void process()
		{
			synchronized(_locker)
			{
				generate();
				ready=true;
				_locker.notifyAll();
				return;
			}
		}
		@Override
		public String out()
		{
			synchronized(_locker)
			{
				while (!ready)try{_locker.wait();}catch(InterruptedException e){}
				ready=false;
				return new Integer(_move).toString();
			}
		}
		@Override
		public void event(MotionEvent m){}
		public aiplayer(int color)
		{
			_color=color;
		}
		void generate()
		{
			rootnegamax(Integer.MIN_VALUE,Integer.MAX_VALUE,1);
			Boolean gen=true;
			for (Integer id:_ply._chgs.keySet())
				if (_ply._chgs.get(id).getValue()!=0){_move=id;gen=false;break;}
			if (gen)for (Integer id: _brd._pnts.keySet()) if(_brd._pnts.get(id)._color==0)_move=id;
		}
		void rootnegamax( int alpha, int beta, int depth )
		{
			int score;
			for (play mv : moves())
			{
				mv.redo(_brd);
				score = -negamax( -beta, -alpha, depth - 1 );
				mv.undo(_brd);
				if( score > alpha )
				{
					_ply=mv;
					alpha = score;
				}
			}
		}
		int negamax( int alpha, int beta, int depth )
		{
			if ( depth == 0) return evaluate();
			int score;
			for (play mv : moves())
			{
				mv.redo(_brd);
				score = -negamax( -beta, -alpha, depth - 1 );
				mv.undo(_brd);
				if( score >= beta ) return beta;
				if( score > alpha ) alpha = score;
			}
			return alpha;
		}
		public int evaluate()
		{
			int sum=0;
			for (point p : _brd._pnts.values())
			{
				if (p._color==1) sum++;
				if (p._color==-1) sum--;
			}
			return -sum;
		}
		public Stack<play> moves()
		{
			Random r = new Random();
			ArrayList<Integer> ids=new ArrayList<Integer>(_brd._pnts.keySet());
			ArrayList<Integer> randomids=new ArrayList<Integer>();
			while (!ids.isEmpty())
			{
				int id=r.nextInt(ids.size());
				randomids.add(ids.get(id));
				ids.remove(id);
			}			
			Stack<play> mvs = new Stack<play>();
			for (int id : randomids)
			{
				play ply=move(id,_color);
				if (ply!=null)mvs.push(ply);
			}
			play ply=new play();
			ply._chgs=new HashMap<Integer,AbstractMap.SimpleEntry<Integer,Integer>>();
			mvs.push(ply);
			return mvs;
		}
		
		public play move(int id, int color)
		{
			standard_board brd=(standard_board)_brd;
			int x=id/brd._size;
			int y=id%brd._size;
			if (brd._pnts.get(id)._color!=0) return null;
			HashMap<Integer,AbstractMap.SimpleEntry<Integer,Integer>> chgs = new HashMap<Integer,AbstractMap.SimpleEntry<Integer,Integer>>();
			brd.move(id,color);
			chgs.put(id, new AbstractMap.SimpleEntry<Integer,Integer>(0,color));
			if (y>0&&brd._pnts.get(id-1)._color==-color) for(int badid : standard_board_clean(brd,id-1)){brd.move(badid,0);chgs.put(badid, new AbstractMap.SimpleEntry<Integer,Integer>(-color,0));}
			if (y<brd._size-1&&brd._pnts.get(id+1)._color==-color) for(int badid : standard_board_clean(brd,id+1)){brd.move(badid,0);chgs.put(badid, new AbstractMap.SimpleEntry<Integer,Integer>(-color,0));}
			if (x>0&&brd._pnts.get(id-brd._size)._color==-color) for(int badid : standard_board_clean(brd,id-brd._size)){brd.move(badid,0);chgs.put(badid, new AbstractMap.SimpleEntry<Integer,Integer>(-color,0));}
			if (x<brd._size-1&&brd._pnts.get(id+brd._size)._color==-color) for(int badid : standard_board_clean(brd,id+brd._size)){brd.move(badid,0);chgs.put(badid, new AbstractMap.SimpleEntry<Integer,Integer>(-color,0));}
			ArrayList<Integer> suicide = standard_board_clean(brd,id);
			if (suicide.size()!=0){chgs.remove(id);brd.move(id,0);}
			for(int badid : suicide){if (badid!=id){brd.move(badid,0);chgs.put(badid, new AbstractMap.SimpleEntry<Integer,Integer>(color,0));}}
			play ply=new play();
			ply._chgs=chgs;
			ply.undo(brd);
			if (_game._ply._chgs!=null&&_game._ply._chgs.size()==2&&chgs.size()==2&&Arrays.deepEquals(_game._ply._chgs.keySet().toArray(),chgs.keySet().toArray()))return null;
			return ply;
		}
	}	
	
	

	public class viewboard extends standard_board
	{
		int _intv,_inix=0;
		public viewboard(int size){super(size);}
		public void draw(Canvas canvas)
		{
			int x=canvas.getClipBounds().width();
			int y=canvas.getClipBounds().height();
			_intv = (y<x?y:x)/(_size+1);
			_inix = (x-(y<x?y:x)*(_size-1)/(_size+1))/2;
			Paint white = new Paint();
			white.setARGB(255,255,255,255);
			Paint black = new Paint();
			black.setARGB(255,0,0,0);
			Paint light = new Paint();
			light.setARGB(255,200,200,200);
			canvas.drawPaint(white);
			for (int xin=1;xin<=_size;xin++)
			{
				canvas.drawLine(_inix+(xin-1)*_intv,_intv,_inix+(xin-1)*_intv,_size*_intv,black);
			}
			for (int yin=1;yin<=_size;yin++)
			{
				canvas.drawLine(_inix,yin*_intv,_inix+(_size-1)*_intv,yin*_intv,black);
			}
			for (int xin=0;xin<_size;xin++)
			{
				for (int yin=0;yin<_size;yin++)
				{
					if (_pnts.get(xin*_size+yin)._color==1) canvas.drawCircle((float)(_inix+xin*_intv),(float)(yin+1)*_intv,((float)_intv)/2,black);
					if (_pnts.get(xin*_size+yin)._color==-1) canvas.drawCircle((float)(_inix+xin*_intv),(float)(yin+1)*_intv,((float)_intv)/2,light);
				}
			}
		}
	}
	public class viewplayer extends player
	{
		LinkedList<Thread> _evntqueue;
		LinkedList<Thread> _outqueue;
		Boolean _ready=false;
		Integer _id;
		viewarbiter _vwabtr;
		Object _locker=new Object();
		@Override
		public void in(string input)
		{
			Thread procthrd = new Thread(new Runnable(){public void run(){process();}});
			_evntqueue.add(procthrd);
			_outqueue.add(procthrd);
			procthrd.start();
		}
		@Override
		public void process()
		{
			synchronized(_locker)
			{
				while(!_ready)
				{
					try{_locker.wait();}
					catch(InterruptedException e){}
				}
				_ready=false;
				Thread.currentThread().setName(_id.toString());
			}
			_evntqueue.remove(Thread.currentThread());
		}
		@Override
		public String out()
		{
			try
			{
				Thread procthrd=_outqueue.poll();
				if (procthrd==null)return null;
				procthrd.join();
				return procthrd.getName();
			}
			catch(InterruptedException e)
			{
				return null;
			}
		}
		public void event(MotionEvent event)
		{
			if (_evntqueue.isEmpty()) return;
			if ((event.getAction()|MotionEvent.ACTION_UP)!=event.getAction()) return;
			viewboard brd=_vwabtr._vbrd;
			float x=event.getX();
			float y=event.getY();
			if (x<brd._inix-0.5*brd._intv||x>=brd._inix+(brd._size-0.5)*brd._intv) return;
			if (y<brd._intv*0.5||y>=(brd._size+0.5)*brd._intv) return;
			_id=(brd._size*java.lang.Math.round((x-brd._inix)/brd._intv)+java.lang.Math.round(y/brd._intv)-1);
			synchronized(_locker)
			{
				_ready=true;
				_locker.notifyAll();
			}
		}
		public viewplayer()
		{
			_evntqueue=new LinkedList<Thread>();
			_outqueue=new LinkedList<Thread>();
		}
	}
	public class viewarbiter extends arbiter
	{
		viewboard _vbrd;
		ArrayList<viewplayer> _plrs;
		Thread _thrd;
		goview _goview;
		@Override
		public void start()
		{
			_thrd=new Thread(new Runnable()
				{
					public void run()
					{
						for (int turn=0;;turn++)
						{
							do{_plrs.get(turn%2).in(null);}
							while (!move(Integer.parseInt(_plrs.get(turn%2).out()),1-2*(turn%2)));
						}
					}
				});
			_thrd.start();
		}
		@Override
		public void stop()
		{
			_thrd.stop();
		}
		@Override
		public void attach(player player)
		{
			try
			{
				if (_plrs.size()<2) _plrs.add((viewplayer)player);
				((viewplayer)player)._vwabtr=this;
				player._brd=_brd;
				player._game=_game;
			}
			catch(Exception e){}
		}
		public viewarbiter(int size, goview goview,viewplayer viewplayer1, viewplayer viewplayer2)
		{
			_game=new game(new play());
			_plrs=new ArrayList<viewplayer>();
			_goview=goview;
			_brd=_vbrd=new viewboard(size);
			attach(viewplayer1);
			attach(viewplayer2);
		}
		public Boolean move(int id, int color)
		{
			int x=id/_vbrd._size;
			int y=id%_vbrd._size;
			if (_vbrd._pnts.get(id)._color!=0) return false;
			HashMap<Integer,AbstractMap.SimpleEntry<Integer,Integer>> chgs = new HashMap<Integer,AbstractMap.SimpleEntry<Integer,Integer>>();
			_vbrd.move(id,color);
			chgs.put(id, new AbstractMap.SimpleEntry<Integer,Integer>(0,color));
			if (y>0&&_vbrd._pnts.get(id-1)._color==-color) for(int badid : standard_board_clean(_vbrd,x*_vbrd._size+y-1)){_vbrd.move(badid,0);chgs.put(badid, new AbstractMap.SimpleEntry<Integer,Integer>(-color,0));}
			if (y<_vbrd._size-1&&_vbrd._pnts.get(id+1)._color==-color) for(int badid : standard_board_clean(_vbrd,x*_vbrd._size+y+1)){_vbrd.move(badid,0);chgs.put(badid, new AbstractMap.SimpleEntry<Integer,Integer>(-color,0));}
			if (x>0&&_vbrd._pnts.get(id-_vbrd._size)._color==-color) for(int badid : standard_board_clean(_vbrd,(x-1)*_vbrd._size+y)){_vbrd.move(badid,0);chgs.put(badid, new AbstractMap.SimpleEntry<Integer,Integer>(-color,0));}
			if (x<_vbrd._size-1&&_vbrd._pnts.get(id+_vbrd._size)._color==-color) for(int badid : standard_board_clean(_vbrd,(x+1)*_vbrd._size+y)){_vbrd.move(badid,0);chgs.put(badid, new AbstractMap.SimpleEntry<Integer,Integer>(-color,0));}
			ArrayList<Integer> suicide=standard_board_clean(_vbrd,id);
			if (suicide.size()>0){chgs.remove(id);_vbrd.move(id,0);}
			for(int badid : suicide){if (badid!=id){_vbrd.move(badid,0);chgs.put(badid, new AbstractMap.SimpleEntry<Integer,Integer>(color,0));}}
			_goview.postInvalidate();
			play ply=new play();
			ply._chgs=chgs;
			game gm=new game(ply);
			game_attach(gm,_game);
			_game=gm;
			return true;
		}
	}
	
	public class standard_board extends board
	{
		int _size;
		public standard_board(int size)
		{
			_size=size;
			_pnts=new HashMap<Integer,point>();
			int id=0;
			for (int xin=0;xin<size;xin++)
			{
				for (int yin=0;yin<size;yin++)
				{
					_pnts.put(id,new point(0));
					if (xin>0) point_connect(_pnts.get(id),_pnts.get(id-size));
					if (yin>0) point_connect(_pnts.get(id),_pnts.get(id-1));
					id++;
				}
			}
		}
	}
	public ArrayList<Integer> standard_board_clean(standard_board stdbrd,int id)
	{
		int color=stdbrd._pnts.get(id)._color;
		int x,y;
		ArrayList<Integer> fnd=new ArrayList<Integer>();
		Stack<Integer> news=new Stack<Integer>();
		Stack<Boolean> a=new Stack<Boolean>();
		Stack<Boolean> b=new Stack<Boolean>();
		Stack<Boolean> c=new Stack<Boolean>();
		Stack<Boolean> d=new Stack<Boolean>();
		int fx,fy;
		news.push(id);
		while (!news.empty())
		{
			id=news.pop();
			x=id/stdbrd._size;
			y=id%stdbrd._size;
			a.push(y!=0);
			b.push(y!=stdbrd._size-1);
			c.push(x!=0);
			d.push(x!=stdbrd._size-1);
			for (int fid : fnd)
			{
				fx=fid/stdbrd._size;
				fy=fid%stdbrd._size;
				if (fx==x&&fy==y-1) {a.pop();a.push(false);}
				if (fx==x&&fy==y+1) {b.pop();b.push(false);}
				if (fx==x-1&&fy==y) {c.pop();c.push(false);}
				if (fx==x+1&&fy==y) {d.pop();d.push(false);}
			}
			if (a.peek()&&stdbrd._pnts.get(id-1)._color==0) return new ArrayList<Integer>();
			if (b.peek()&&stdbrd._pnts.get(id+1)._color==0) return new ArrayList<Integer>();
			if (c.peek()&&stdbrd._pnts.get(id-stdbrd._size)._color==0) return new ArrayList<Integer>();
			if (d.peek()&&stdbrd._pnts.get(id+stdbrd._size)._color==0) return new ArrayList<Integer>();
			fnd.add(id);
			if (a.pop()&&stdbrd._pnts.get(id-1)._color==color) news.push(id-1);
			if (b.pop()&&stdbrd._pnts.get(id+1)._color==color) news.push(id+1);
			if (c.pop()&&stdbrd._pnts.get(id-stdbrd._size)._color==color) news.push(id-stdbrd._size);
			if (d.pop()&&stdbrd._pnts.get(id+stdbrd._size)._color==color) news.push(id+stdbrd._size);
			news.removeAll(fnd);
		}
		return fnd;
	}
	
	
	public class point
	{
		int _color;
		point[] _adjs;
		public point(int color)
		{
			_color=color;
			_adjs=new point[0];
		}
	}
	static public void point_connect(point point1, point point2)
	{
		point1._adjs=Arrays.copyOf(point1._adjs,point1._adjs.length+1);
		point2._adjs=Arrays.copyOf(point2._adjs,point2._adjs.length+1);
		point1._adjs[point1._adjs.length-1]=point2;
		point2._adjs[point2._adjs.length-1]=point1;
	}
	
	public class board
	{
		HashMap<Integer,point> _pnts;
		public void move(int id, int color)
		{
			_pnts.get(id)._color=color;
		}
	}
	
	public class play
	{
		HashMap<Integer,AbstractMap.SimpleEntry<Integer,Integer>> _chgs;
		public void redo(board board)
		{
			 for(int id : _chgs.keySet())
			 {
				 board.move(id, _chgs.get(id).getValue());
			 }
		}
		public void undo(board board)
		{
			for(int id : _chgs.keySet())
			{
				board.move(id, _chgs.get(id).getKey());
			}
		}
	}

	public class game
	{
		play _ply;
		game _prnt;
		game[] _cdrn;
		public game(play play)
		{
			_ply=play;
			_prnt=null;
			_cdrn=new game[0];
		}
	}
	static public void game_attach(game child, game parent)
	{
		if (child._prnt!=null) return;
		child._prnt=parent;
		parent._cdrn=Arrays.copyOf(parent._cdrn,parent._cdrn.length+1);
		parent._cdrn[parent._cdrn.length-1]=child;
	}
	
	public class arbiter
	{
		board _brd;
		game _game;
		public void start(){}
		public void stop(){}
		public void attach(player player){}
	}
	public class player
	{
		board _brd;
		game _game;
		public void in(string input){}
		public void process(){}
		public String out(){return null;}
	}
}
