package com.example.picture_scramble;

import java.util.Random;

import com.example.picture_scramble.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static final String MOVE_COUNT_TEXT = "Moves: ";
	private static final String BEST_COUNT_TEXT = "Best: ";
	private static final int[] PICTURES = {
		R.drawable.pic1,
		R.drawable.pic2,
		R.drawable.pic3,
		R.drawable.pic4,
		R.drawable.pic5
	};
	private static Random rand = new Random();
	
	private int blockSize;
	private int blocks = 3;
	private int moves = 0;
	private int currentPicture = 0;
	private int highScore;
	
	private TextView moveCount;
	private TextView bestCount;
	private NumberPicker numberPicker;
	private TextView winMessage;
	private SharedPreferences prefs;
	
	Point[] currentPositions;
	Point[] homePositions;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		moveCount = (TextView) findViewById(R.id.moveCount);
		moveCount.setText(MOVE_COUNT_TEXT + moves);
		
		bestCount = (TextView) findViewById(R.id.bestCount);
		
		numberPicker = (NumberPicker) findViewById(R.id.puzzle_selector);
		numberPicker.setMinValue(1);
		numberPicker.setMaxValue(PICTURES.length);
		numberPicker.setValue(1);
		
		winMessage = (TextView) findViewById(R.id.winMessage);
		winMessage.setVisibility(View.GONE);
		
		prefs = getPreferences(MODE_PRIVATE);
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		generatePuzzle();
	}
	
	private void generatePuzzle() {
		
		homePositions = new Point[blocks * blocks];
		currentPositions = new Point[blocks * blocks];
		Point[][] randPosition;
		
		//fetch a set of random coordinates for each puzzle piece.
		//while the puzzle is unsolvable, continue to search for random positions until a valid set of coordinates is found.
		do {
			randPosition = setRandomCoordinates();
		} while (validateRandomPuzzle(randPosition));
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.puzzle_view);
		layout.removeAllViews();
		
		Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), PICTURES[currentPicture]);
		Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, layout.getWidth()+10, layout.getHeight()+10, true);
		blockSize = scaledBitmap.getWidth() / blocks;
		
		updateBest();
		
		for (int x = 0; x < blocks; x++) {
			for (int y = 0; y < blocks; y++) {
				
				Bitmap chunk = Bitmap.createBitmap(scaledBitmap, x * blockSize, y * blockSize, blockSize, blockSize);
				
				int rx = randPosition[x][y].x;
				int ry = randPosition[x][y].y;
				int tagId = (ry * blocks) + rx;
				
				homePositions[tagId] = new Point();
				homePositions[tagId].x = x;
				homePositions[tagId].y = y;
				
				currentPositions[tagId] = new Point();
				currentPositions[tagId].x = rx;
				currentPositions[tagId].y = ry;
				
				
				ImageView view = new ImageView(this);
				view.setImageBitmap(chunk);
				view.setId(tagId);
				view.setX(rx * blockSize);
				view.setY(ry * blockSize);
				view.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						MainActivity act = MainActivity.this;
						
						int tag = v.getId();
						Point dir = act.getEmptyNeighbor(tag);
						
						if (dir.x == 0 && dir.y == 0) {
							return;
						}
						int newTag = act.newTag(dir, tag);
						
						int newX = (int) (v.getX() + (blockSize * dir.x));
						int newY = (int) (v.getY() + (blockSize * dir.y));
						
						act.homePositions[newTag].x = act.homePositions[tag].x;
						act.homePositions[newTag].y = act.homePositions[tag].y;
						
						act.currentPositions[newTag].x = act.currentPositions[tag].x + dir.x;
						act.currentPositions[newTag].y = act.currentPositions[tag].y + dir.y;
						
						act.homePositions[tag].x = -1;
						act.homePositions[tag].y = -1;
						
						act.currentPositions[tag].x = -1;
						act.currentPositions[tag].y = -1;
						
						v.setX(newX);
						v.setY(newY);
						v.setId(newTag);
						
						act.moveCount.setText(MOVE_COUNT_TEXT + (++moves));
						act.checkGameComplete();
					}
				});
				
				
				if (!(x == blocks-1 && y == blocks-1)) {
					layout.addView(view, params);
				} else {
					currentPositions[tagId].x = -1;
					currentPositions[tagId].y = -1;
					
					homePositions[tagId].x = -1;
					homePositions[tagId].y = -1;
				}
			}
		}
	}
	
	private void updateBest() {
		highScore = prefs.getInt(getHighScoreTag(), 9999);
		bestCount.setText(BEST_COUNT_TEXT + highScore);
	}
	
	private String getHighScoreTag() {
		return "" + currentPicture + blocks;
	}
	
	public void newGame(View view) {
		switch (((RadioGroup) findViewById(R.id.difficulty_group)).getCheckedRadioButtonId()) {
			case R.id.radio_easy:
				blocks = 3;
				break;
			case R.id.radio_med:
				blocks = 5;
				break;	
			case R.id.radio_hard:
				blocks = 9;
				break;
		}
		
		moves = 0;
		moveCount.setText(MOVE_COUNT_TEXT + moves);
		currentPicture = numberPicker.getValue() - 1;
		winMessage.setVisibility(View.GONE);
		
		generatePuzzle();
	}
	
	private void gameOver() {
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.puzzle_view);
		layout.removeAllViews();
		
		Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), PICTURES[currentPicture]);
		Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, layout.getWidth()+10, layout.getHeight()+10, true);
		
		if (moves < highScore) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt(getHighScoreTag(), moves);
			editor.commit();
			updateBest();
			Toast.makeText(this, "You got a new high score!", Toast.LENGTH_LONG).show();
		}
		
		ImageView view = new ImageView(this);
		view.setImageBitmap(scaledBitmap);
		layout.addView(view);
		winMessage.setVisibility(View.VISIBLE);
	}
	
	private void checkGameComplete() {
		for (int x = 0; x < blocks; x++) {
			for (int y = 0; y < blocks; y++) {
				int tag = (y * blocks) + x;
				Point cur = currentPositions[tag];
				Point home = homePositions[tag];
				if (!(cur.x == home.x && cur.y == home.y)) {
					return;
				}
			}
		}
		
		gameOver();
	}
	
	private int newTag(Point point, int oldTag) {
		return oldTag + point.x + (point.y * blocks);
	}
	
	private Point getEmptyNeighbor(int tag) {
		int totalBlocks = blocks * blocks;
		Point direction = new Point(0, 0);
		int rowPos = tag % blocks;
		
		if (tag-1 >= 0) {
	        //if the neighbor is in bounds, check if it is empty.
	        //Also make sure the current piece isn't the first in its row.
	        if (currentPositions[tag - 1].x == -1 && rowPos > 0) {
	            //neighbor to right is empty, move in that direction
	            direction.x = -1;
	            direction.y = 0;
	        }
	    }
		//Same setup as above, except check the neighbor to the left.
	    if (tag+1 < totalBlocks) {
	        if (currentPositions[tag + 1].x == -1 && rowPos < blocks - 1) {
	            direction.x = 1;
	            direction.y = 0;
	        }
	    }
	    //Check the neighbor above.
	    if (tag-blocks >= 0) {
	        //We do not need to worry above checking the row position when looking for neighbors above/below.
	        if (currentPositions[tag-blocks].x == -1) {
	            direction.x = 0;
	            direction.y = -1;
	        }
	    }
	    //check the neighbor below.
	    if (tag+blocks < totalBlocks) {
	        if (currentPositions[tag+blocks].x == -1) {
	            direction.x = 0;
	            direction.y = 1;
	        }
	    }
	    
	    return direction;
	}
	
	
	private Point[][] setRandomCoordinates() {
		int[][] taken = new int[blocks][blocks];
		Point[][] randPositions = new Point[blocks][blocks];
		
		for (int i = 0; i < blocks; i++) {
			for (int j = 0; j < blocks; j++) {
				while (true) {
					int rx = rand.nextInt(blocks);
					int ry = rand.nextInt(blocks);
					
					if (taken[rx][ry] != 1) {
						taken[rx][ry] = 1;
						randPositions[i][j] = new Point();
						randPositions[i][j].x = rx;
						randPositions[i][j].y = ry;
						break;
					}
				}
			}
		}

		return randPositions;
	}
	
	private boolean validateRandomPuzzle(Point[][] puzzle) {
		int inversions = 0;
		
		for (int i = 0; i < blocks; i++) {
			for (int j = 0; j < blocks; j++) {
				if (!(i == blocks-1 && j == blocks-1)) {
					int current = (puzzle[i][j].y * blocks) + puzzle[i][j].x;
					int startCount = j + 1;
					
					for (int x = i; x < blocks; x++) {
						for (int y = startCount; y < blocks; y++) {
							int toCheck = (puzzle[x][y].y * blocks) + puzzle[x][y].x;
							
							if (!(x == blocks-1 && y == blocks-1)) {
								if (current > toCheck) {
									inversions++;
								}
							}
						}
						startCount = 0;
					}
				}
			}
		}
			
			
			
		return inversions % 2 == 0;
	}
}
