package ru.spbstu.khramov;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CellularAutomaton {
	private final long rule;
	private boolean[][] cells;
	private final int size;
	private boolean[][] next;

	private int generation;

	private final Map<BitSet, Integer> history = new HashMap<>();
	private boolean cycleFound;
	private int transientLength;
	private int period;

	public CellularAutomaton(int numberOption, int constValue, int year, int month, int day, int size) {
		this.rule = (long) numberOption * constValue * year * month * day;
		this.size = size;
		this.cells = new boolean[this.size][this.size];
		this.next = new boolean[this.size][this.size];
	}

	int newState(int idx) {
		return bit(rule, idx);
	}

	static int bit(long x, int k) {
		long shifted = x >> k;
		long lastBit = shifted & 1;
		return (int) lastBit;
	}

	public boolean getCell(int row, int col) {
		return cells[row][col];
	}

	public void setCell(int row, int col, boolean value) {
		cells[row][col] = value;
	}

	private int wrap(int coord) {
		return (coord % size + size) % size;
	}

	private boolean getWrapped(int row, int col) {
		return cells[wrap(row)][wrap(col)];
	}

	private static int toBit(boolean value) {
		return value ? 1 : 0;
	}

	int neighborhoodIndex(int row, int col) {
		int n = toBit(getWrapped(row - 1, col));
		int c = toBit(getWrapped(row, col));
		int s = toBit(getWrapped(row + 1, col));
		int w = toBit(getWrapped(row, col - 1));
		int e = toBit(getWrapped(row, col + 1));
		return c * 16 + n * 8 + e * 4 + w * 2 + s;
	}

	public int getGeneration() {
		return generation;
	}

	public int getSize() {
		return size;
	}

	public void step() {
		if (history.isEmpty()) {
			history.put(snapshot(), generation);
		}

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				int idx = neighborhoodIndex(i, j);
				next[i][j] = newState(idx) == 1;
			}
		}

		boolean[][] tmp = cells;
		cells = next;
		next = tmp;

		generation++;
		checkCycle();
	}

	private void checkCycle() {
		if (cycleFound) {
			return;
		}
		Integer prev = history.putIfAbsent(snapshot(), generation);
		if (prev != null) {
			cycleFound = true;
			transientLength = prev;
			period = generation - prev;
		}
	}

	private BitSet snapshot() {
		BitSet bits = new BitSet(size * size);
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				if (cells[row][col]) {
					bits.set(row * size + col);
				}
			}
		}
		return bits;
	}

	public boolean isCycleFound() {
		return cycleFound;
	}

	public int getTransientLength() {
		return transientLength;
	}

	public int getPeriod() {
		return period;
	}

	public int population() {
		int count = 0;
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				if (cells[row][col]) {
					count++;
				}
			}
		}
		return count;
	}

	public void clear() {
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				cells[row][col] = false;
			}
		}
	}

	public void randomize(double density, long seed) {
		Random random = new Random(seed);
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				cells[row][col] = random.nextDouble() < density;
			}
		}
	}

}
