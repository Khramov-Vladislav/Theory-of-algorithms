package ru.spbstu.khramov;

import static java.lang.IO.*;

public class Main {
	private Main() {
	}

	static void main() {
		println("Двумерный клеточный автомат");
		println();

		int size = readInt("Ширина поля (3...100)", 3, 100, 20);
		int iterations = readInt("Количество итераций (1...100000)", 1, 100_000, 10);

		CellularAutomaton automaton = new CellularAutomaton(43, 11, 2006, 11, 2, size);
		initField(automaton);

		println();
		int printEvery = readInt("Печатать каждое k-е поколение, k", 1, 100_000, 1);
		boolean stopOnCycle = readInt("Остановиться, когда найден цикл? (1 - да, 0 - нет)", 0, 1, 1) == 1;

		println();
		printGeneration(automaton);
		for (int i = 1; i <= iterations; i++) {
			boolean hadCycle = automaton.isCycleFound();
			automaton.step();
			boolean cycleJustFound = !hadCycle && automaton.isCycleFound();

			if (i % printEvery == 0 || i == iterations || cycleJustFound) {
				printGeneration(automaton);
			}
			if (cycleJustFound && stopOnCycle) {
				break;
			}
		}

		printSummary(automaton);
	}

	static void printGeneration(CellularAutomaton automaton) {
		String header = "Поколение " + automaton.getGeneration() + " | живых: " + automaton.population();
		if (automaton.isCycleFound()) {
			header += " | цикл: предпериод = " + automaton.getTransientLength() + ", период = " + automaton.getPeriod();
		}
		println(header);
		printField(automaton);
	}

	static void printSummary(CellularAutomaton automaton) {
		int size = automaton.getSize();
		println("Итог");
		println("Выполнено итераций: " + automaton.getGeneration());
		println("Живых клеток: " + automaton.population() + " из " + size * size);
		if (automaton.isCycleFound()) {
			int t = automaton.getTransientLength();
			int p = automaton.getPeriod();
			println("Цикл найден на поколении " + (t + p));
			println("Предпериод: " + t);
			println("Период: " + p + (p == 1 ? " (неподвижная точка)" : ""));
		} else {
			println("Цикл за " + automaton.getGeneration() + " итераций не обнаружен.");
		}
	}

	static void initField(CellularAutomaton automaton) {
		println();
		println("Начальные условия:");
		println("  1 - случайно");
		println("  2 - вручную");
		println("  3 - одна клетка в центре");
		println("  4 - пустое поле");
		int mode = readInt("Выбор", 1, 4, 1);

		switch (mode) {
			case 1 -> {
				int percent = readInt("Плотность живых клеток, % (0...100)", 0, 100, 50);
				long seed = readSeed();
				automaton.randomize(percent / 100.0, seed);
			}
			case 2 -> readManual(automaton);
			case 3 -> {
				int center = automaton.getSize() / 2;
				automaton.setCell(center, center, true);
			}
			default -> automaton.clear();
		}
	}

	static long readSeed() {
		while (true) {
			String s = readLine("Seed (пусто = случайный): ");
			if (s.isEmpty()) {
				long seed = System.nanoTime();
				println("Используется seed = " + seed);
				return seed;
			}
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException e) {
				println("  Введите целое число или оставьте пустым.");
			}
		}
	}

	static void readManual(CellularAutomaton automaton) {
		int size = automaton.getSize();
		println("Вводите строки поля сверху вниз (до " + size + " строк).");
		println("Живая клетка: + или 1, мёртвая: . или 0. Пустая строка — закончить ввод.");
		automaton.clear();
		for (int row = 0; row < size; row++) {
			String line = readln(String.format("%3d> ", row));
			if (line == null || line.isBlank()) {
				break;
			}
			for (int col = 0; col < Math.min(line.length(), size); col++) {
				char ch = line.charAt(col);
				automaton.setCell(row, col, ch == '+' || ch == '1');
			}
		}
	}

	static int readInt(String prompt, int min, int max, int def) {
		while (true) {
			String s = readLine(prompt + " [" + def + "]: ");
			if (s.isEmpty()) {
				return def;
			}
			try {
				int value = Integer.parseInt(s);
				if (value >= min && value <= max) {
					return value;
				}
			} catch (NumberFormatException ignored) {
			}
			println("  Введите целое число от " + min + " до " + max + ".");
		}
	}

	static String readLine(String prompt) {
		String s = readln(prompt);
		return s == null ? "" : s.trim();
	}

	static void printField(CellularAutomaton automaton) {
		for (int row = 0; row < automaton.getSize(); row++) {
			for (int col = 0; col < automaton.getSize(); col++) {
				print(automaton.getCell(row, col) ? "+ " : ". ");
			}
			println();
		}
		println();
	}

}
