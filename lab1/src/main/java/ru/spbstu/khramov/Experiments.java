package ru.spbstu.khramov;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.IO.*;

/**
 * Пакетные эксперименты для анализа автомата: много прогонов без диалога с пользователем.
 * Все параметры передаются аргументами командной строки, seed перебираются 1, 2, 3, ...,
 * поэтому любой результат можно воспроизвести.
 * <pre>
 * periods    [maxSize] [seeds] [maxSteps]  — предпериоды и периоды для n = 3..maxSize
 * population [size] [steps] [seeds]        — средняя доля живых клеток во времени
 * damage     [size] [steps] [seeds]        — расстояние Хэмминга после изменения одной клетки
 * </pre>
 */
public class Experiments {
	/**
	 * Класс содержит только статические методы, создавать его объекты не нужно.
	 */
	private Experiments() {
	}

	/**
	 * Точка входа: выбирает эксперимент по первому аргументу, остальные аргументы — его параметры
	 * (если не заданы, берутся значения по умолчанию).
	 *
	 * @param args название эксперимента и его параметры
	 */
	static void main(String[] args) {
		String mode = args.length > 0 ? args[0] : "";
		switch (mode) {
			case "periods" -> periods(arg(args, 1, 12), arg(args, 2, 100), arg(args, 3, 100_000));
			case "population" -> population(arg(args, 1, 50), arg(args, 2, 1000), arg(args, 3, 20));
			case "damage" -> damage(arg(args, 1, 100), arg(args, 2, 400), arg(args, 3, 10));
			default -> {
				println("Использование: periods [maxSize] [seeds] [maxSteps]");
				println("               population [size] [steps] [seeds]");
				println("               damage [size] [steps] [seeds]");
			}
		}
	}

	/**
	 * Возвращает аргумент с номером i как число или значение по умолчанию, если аргумента нет.
	 *
	 * @param args аргументы командной строки
	 * @param i    номер аргумента
	 * @param def  значение по умолчанию
	 * @return число из аргумента или def
	 */
	static int arg(String[] args, int i, int def) {
		return args.length > i ? Integer.parseInt(args[i]) : def;
	}

	/**
	 * Создаёт автомат с правилом из задания и пустым полем.
	 *
	 * @param size ширина поля
	 * @return новый автомат
	 */
	static CellularAutomaton newAutomaton(int size) {
		return new CellularAutomaton(43, 11, 2006, 11, 2, size);
	}

	// ---------------- эксперимент 1: предпериод и период ----------------

	/**
	 * Для каждого n = 3..maxSize запускает seeds случайных полей (плотность 50 %, seed = 1..seeds)
	 * до нахождения цикла или maxSteps шагов. Печатает, сколько циклов найдено, статистику предпериода
	 * и распределение периодов (период → сколько раз встретился).
	 *
	 * @param maxSize  наибольшая ширина поля
	 * @param seeds    число прогонов для каждой ширины
	 * @param maxSteps ограничение на число шагов одного прогона
	 */
	static void periods(int maxSize, int seeds, int maxSteps) {
		println("Предпериод и период: плотность 50 %, seed = 1.." + seeds + ", не более " + maxSteps + " шагов");
		for (int size = 3; size <= maxSize; size++) {
			int found = 0;
			long transientSum = 0;
			int transientMax = 0;
			Map<Integer, Integer> periodCount = new TreeMap<>(); // период -> число прогонов

			for (int seed = 1; seed <= seeds; seed++) {
				CellularAutomaton automaton = newAutomaton(size);
				automaton.randomize(0.5, seed);
				for (int i = 0; i < maxSteps && !automaton.isCycleFound(); i++) {
					automaton.step();
				}
				if (automaton.isCycleFound()) {
					found++;
					transientSum += automaton.getTransientLength();
					transientMax = Math.max(transientMax, automaton.getTransientLength());
					periodCount.merge(automaton.getPeriod(), 1, Integer::sum);
				}
			}

			String line = String.format("n = %2d | найдено %d/%d", size, found, seeds);
			if (found > 0) {
				line += String.format(Locale.US, " | предпериод: средний %.1f, макс %d | периоды: ",
						(double) transientSum / found, transientMax);
				StringBuilder periodsText = new StringBuilder();
				for (Map.Entry<Integer, Integer> e : periodCount.entrySet()) {
					periodsText.append(e.getKey()).append("×").append(e.getValue()).append(" ");
				}
				line += periodsText.toString().trim();
			}
			println(line);
		}
	}

	// ---------------- эксперимент 2: доля живых клеток ----------------

	/**
	 * Для плотностей 10, 30, 50, 70, 90 % считает среднюю по seeds прогонам долю живых клеток
	 * в каждом поколении. Печатает таблицу (каждое 10-е поколение) в формате для pgfplots:
	 * {@code t d10 d30 d50 d70 d90}.
	 *
	 * @param size  ширина поля
	 * @param steps число поколений
	 * @param seeds число прогонов для каждой плотности
	 */
	static void population(int size, int steps, int seeds) {
		int[] densities = {10, 30, 50, 70, 90};
		double[][] average = new double[densities.length][steps + 1];

		for (int d = 0; d < densities.length; d++) {
			for (int seed = 1; seed <= seeds; seed++) {
				CellularAutomaton automaton = newAutomaton(size);
				automaton.randomize(densities[d] / 100.0, seed);
				for (int t = 0; t <= steps; t++) {
					average[d][t] += automaton.population() / (double) (size * size) / seeds;
					automaton.step();
				}
			}
		}

		print("t");
		for (int density : densities) {
			print(" d" + density);
		}
		println();
		for (int t = 0; t <= steps; t += 10) {
			print(t);
			for (int d = 0; d < densities.length; d++) {
				print(String.format(Locale.US, " %.4f", average[d][t]));
			}
			println();
		}
	}

	// ---------------- эксперимент 3: распространение возмущения ----------------

	/**
	 * Запускает пары автоматов с одинаковым случайным полем (плотность 50 %), но во втором
	 * изменена одна центральная клетка. Печатает среднее по seeds прогонам расстояние Хэмминга
	 * (число различающихся клеток) в каждом 10-м поколении: {@code t h}.
	 *
	 * @param size  ширина поля
	 * @param steps число поколений
	 * @param seeds число пар
	 */
	static void damage(int size, int steps, int seeds) {
		double[] average = new double[steps + 1];
		int center = size / 2;

		for (int seed = 1; seed <= seeds; seed++) {
			CellularAutomaton original = newAutomaton(size);
			CellularAutomaton perturbed = newAutomaton(size);
			original.randomize(0.5, seed);
			perturbed.randomize(0.5, seed); // тот же seed — то же поле
			perturbed.setCell(center, center, !perturbed.getCell(center, center));

			for (int t = 0; t <= steps; t++) {
				average[t] += hamming(original, perturbed) / (double) seeds;
				original.step();
				perturbed.step();
			}
		}

		println("t h");
		for (int t = 0; t <= steps; t += 10) {
			println(String.format(Locale.US, "%d %.2f", t, average[t]));
		}
	}

	/**
	 * Считает расстояние Хэмминга между полями двух автоматов одного размера.
	 *
	 * @param a первый автомат
	 * @param b второй автомат
	 * @return число клеток, состояния которых различаются
	 */
	static int hamming(CellularAutomaton a, CellularAutomaton b) {
		int distance = 0;
		for (int row = 0; row < a.getSize(); row++) {
			for (int col = 0; col < a.getSize(); col++) {
				if (a.getCell(row, col) != b.getCell(row, col)) {
					distance++;
				}
			}
		}
		return distance;
	}

}
