package bms.player.beatoraja.play;

import bms.model.*;

/**
 * 判定アルゴリズム
 * 
 * @author exch
 */
public enum JudgeAlgorithm {

	/**
	 * 判定アルゴリズム:コンボ最優先
	 */
	Combo {
		@Override
		public boolean compare(Note t1, Note t2, long ptime, int[][] judgetable) {
			return t2.getState() == 0 && t1.getTime() < ptime + judgetable[2][0] && t2.getTime() <= ptime + judgetable[2][1];
		}
	},
	/**
	 * 判定アルゴリズム:判定時間差最優先
	 */
	Duration {
		@Override
		public boolean compare(Note t1, Note t2, long ptime, int[][] judgetable) {
			return Math.abs(t1.getTime() - ptime) > Math.abs(t2.getTime() - ptime) && t2.getState() == 0;
		}
	},
	/**
	 * 判定アルゴリズム:最下ノーツ優先
	 */
	Lowest {
		@Override
		public boolean compare(Note t1, Note t2, long ptime, int[][] judgetable) {
			return false;
		}
	},
	/**
	 * 判定アルゴリズム:スコア最優先
	 */
	Score {
		@Override
		public boolean compare(Note t1, Note t2, long ptime, int[][] judgetable) {
			return t2.getState() == 0 && t1.getTime() < ptime + judgetable[1][0] && t2.getTime() <= ptime + judgetable[1][1];
		}
	}
	;

	private int judge;
	private int laneOffset;

	/**
	 * 判定対象ノーツを取得する
	 *
	 * @param lanemodel レーン
	 * @param ptime 時間
	 * @param judgetable 判定時間テーブル
	 * @param pmsjudge PMS判定
	 * @return 判定対象ノーツ
	 */
	public Note getNote(Lane lanemodel, long ptime, int[][] judgetable, int judgestart, int judgeend, boolean pmsjudge) {
		Note note = null;
		int judge = 0;
		for (Note judgenote = lanemodel.getNote();judgenote != null;judgenote = lanemodel.getNote()) {
			final int dtime = (int) (judgenote.getTime() - ptime);
			if (dtime >= judgeend) {
				break;
			}
			if (dtime >= judgestart) {
				if (!(judgenote instanceof MineNote) && !(judgenote instanceof LongNote
						&& ((LongNote) judgenote).isEnd())) {
					if (note == null || note.getState() != 0 || compare(note, judgenote, ptime, judgetable)) {
						if (!(pmsjudge && (judgenote.getState() != 0
								|| (judgenote.getState() == 0 && judgenote.getPlayTime() != 0 && dtime >= judgetable[2][1])))) {
							if (judgenote.getState() != 0) {
								judge = (dtime >= judgetable[4][0] && dtime <= judgetable[4][1]) ? 5 : 6;
							} else {
								for (judge = 0; judge < judgetable.length && !(dtime >= judgetable[judge][0] && dtime <= judgetable[judge][1]); judge++) {
								}
								judge = (judge >= 4 ? judge + 1 : judge);
							}
							if(judge < 6 && (judge < 4 || note == null || Math.abs(note.getTime() - ptime) > Math.abs(judgenote.getTime() - ptime))) {
								note = judgenote;
							}
						}
					}
				}
			}
		}
		this.judge = judge;
		this.laneOffset = 0;
		return note;
	}

	/**
	 * 判定対象ノーツの判定を取得する
	 * @return 判定
	 */
	public int getJudge() {
		return judge;
	}

	/**
	 * 判定対象ノーツのレーンのずれを取得する
	 */
	public int getLaneOffset() {
		return laneOffset;
	}

	// 空POOR判定がないときのキー音取得(マルチレーン)
	public Note getFreeNoteMultiLane(Lane[] lanes, long ptime, int lane) {
		Note n = null;
		int laneOffset = 0;
		long distance = Long.MAX_VALUE;

		// 隠しノートのうち、通過済みで最も新しいものを取得(とりあえず同一レーンだけとする)
		for (Note note : lanes[lane].getHiddens()) {
			if (note.getTime() >= ptime) {
				break;
			}
			n = note;
			laneOffset = 0;
			distance = getMultiLaneDistance(ptime - note.getTime(), 0);
		}

		for (int i = Math.max(0, lane - 12); i <= Math.min(lanes.length-1, lane + 12); i++) {
			Lane lanemodel =lanes[i];
			Note n1 = null;
			// レーン内で現在位置に最も近いノートを取得
			for (Note note : lanemodel.getNotes()) {
				if (note instanceof LongNote && note.getState() != 0)
					continue;
				if (note.getTime() >= ptime) {
					if (n1 == null || note.getTime() - ptime < ptime - n1.getTime()) {
						n1 = note;
					}
					break;
				} else {
					n1 = note;
				}
			}
			if (n1 != null) {
				long dist = getMultiLaneDistance(n1.getTime() - ptime, i - lane);
				if (dist < distance) {
					n = n1;
					laneOffset = i - lane;
					distance = dist;
				}
			}
		}

		this.laneOffset = laneOffset;
		return n;
	}

	private long getMultiLaneDistance(long timeOffset, int laneOffset) {
		long absTimeOffset = Math.abs(timeOffset);
		long absLaneOffset = Math.abs(laneOffset);
		if (absTimeOffset < 500) {
			// レーン重視
			return absTimeOffset + absLaneOffset * 1000;
		} else if (absTimeOffset < 4000) {
			// 中間
			return absTimeOffset + absLaneOffset * 250;
		} else {
			// 時刻重視
			return absTimeOffset + absLaneOffset * 100;
		}
	}

	/**
	 * ２つのノーツを比較する
	 * @param t1 ノーツ1
	 * @param t2 ノーツ2
	 * @param ptime キー操作の時間
	 * @param judgetable 判定テーブル
	 * @return ノーツ2が選ばれた場合はtrue, ノーツ1が選ばれた場合はfalse
	 */
	public abstract boolean compare(Note t1, Note t2, long ptime, int[][] judgetable);
	
	public static int getIndex(JudgeAlgorithm algorithm) {
		for(int i = 0;i < JudgeAlgorithm.values().length;i++) {
			if(algorithm == JudgeAlgorithm.values()[i]) {
				return i;
			}
		}
		return -1;
	}
}