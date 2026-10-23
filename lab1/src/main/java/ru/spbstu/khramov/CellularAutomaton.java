package ru.spbstu.khramov;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Двумерный клеточный автомат с двумя состояниями клетки, окрестностью фон Неймана
 * и тороидальными граничными условиями.
 * <p>
 * Окрестность клетки C — она сама и 4 соседа по сторонам:
 * <pre>
 *       N
 *     W C E
 *       S
 * </pre>
 * Конфигурация окрестности кодируется индексом в порядке C N E W S:
 * {@code idx = C*16 + N*8 + E*4 + W*2 + S}. Новое состояние клетки — бит номер {@code idx}
 * номера правила.
 * <p>
 * Координаты: {@code row} — номер строки (растёт вниз), {@code col} — номер столбца (растёт вправо).
 */
public class CellularAutomaton {
	private final long rule;
	private boolean[][] cells;
	private final int size;
	private boolean[][] next;

	private int generation;

	// --- обнаружение цикла ---
	// Все встреченные поля: поле -> номер поколения, в котором оно впервые появилось
	private final Map<BitSet, Integer> history = new HashMap<>();
	private boolean cycleFound;
	private int transientLength; // предпериод t
	private int period;          // период p (1 = неподвижная точка)

	/**
	 * Создаёт автомат с пустым (все клетки мёртвые) полем.
	 * Номер правила вычисляется как произведение
	 * {@code numberOption * constValue * year * month * day}.
	 *
	 * @param numberOption номер варианта
	 * @param constValue   константа из формулы задания (11)
	 * @param year         год рождения
	 * @param month        месяц рождения
	 * @param day          день рождения
	 * @param size         ширина (и высота) квадратного поля
	 */
	public CellularAutomaton(int numberOption, int constValue, int year, int month, int day, int size) {
		this.rule = (long) numberOption * constValue * year * month * day;
		this.size = size;
		this.cells = new boolean[this.size][this.size];
		this.next = new boolean[this.size][this.size];
	}

	/**
	 * Возвращает новое состояние клетки по индексу её окрестности, то есть бит номер idx правила.
	 *
	 * @param idx индекс окрестности (0..31), см. {@link #neighborhoodIndex(int, int)}
	 * @return 1 — клетка будет живой, 0 — мёртвой
	 */
	int newState(int idx) {
		return bit(rule, idx);
	}

	/**
	 * Возвращает k-й бит числа x (биты нумеруются справа, с нуля).
	 *
	 * @param x число
	 * @param k номер бита
	 * @return 0 или 1
	 */
	static int bit(long x, int k) {
		long shifted = x >> k;
		long lastBit = shifted & 1;
		return (int) lastBit;
	}

	/**
	 * Возвращает состояние клетки (без учёта тора: координаты должны быть внутри поля).
	 *
	 * @param row номер строки (0..size-1)
	 * @param col номер столбца (0..size-1)
	 * @return true — клетка живая
	 */
	public boolean getCell(int row, int col) {
		return cells[row][col];
	}

	/**
	 * Задаёт состояние клетки (без учёта тора: координаты должны быть внутри поля).
	 *
	 * @param row   номер строки (0..size-1)
	 * @param col   номер столбца (0..size-1)
	 * @param value true — живая, false — мёртвая
	 */
	public void setCell(int row, int col, boolean value) {
		cells[row][col] = value;
	}

	/**
	 * Переводит любую координату в диапазон 0..size-1 по модулю size (тор):
	 * -1 превращается в size-1, size — в 0.
	 *
	 * @param coord координата, возможно вышедшая за край поля
	 * @return координата внутри поля
	 */
	private int wrap(int coord) {
		return (coord % size + size) % size;
	}

	/**
	 * Возвращает состояние клетки с учётом тора: координаты могут выходить за край поля.
	 *
	 * @param row номер строки (любое целое)
	 * @param col номер столбца (любое целое)
	 * @return true — клетка живая
	 */
	private boolean getWrapped(int row, int col) {
		return cells[wrap(row)][wrap(col)];
	}

	/**
	 * Переводит логическое значение в число: true → 1, false → 0.
	 *
	 * @param value логическое значение
	 * @return 1 или 0
	 */
	private static int toBit(boolean value) {
		return value ? 1 : 0;
	}

	/**
	 * Собирает индекс окрестности фон Неймана клетки (row, col) в порядке C N E W S:
	 * {@code idx = C*16 + N*8 + E*4 + W*2 + S}. Соседи за краем поля берутся с противоположной стороны.
	 *
	 * @param row номер строки клетки
	 * @param col номер столбца клетки
	 * @return индекс окрестности 0..31
	 */
	int neighborhoodIndex(int row, int col) {
		int n = toBit(getWrapped(row - 1, col));
		int c = toBit(getWrapped(row, col));
		int s = toBit(getWrapped(row + 1, col));
		int w = toBit(getWrapped(row, col - 1));
		int e = toBit(getWrapped(row, col + 1));
		return c * 16 + n * 8 + e * 4 + w * 2 + s;
	}

	/**
	 * Возвращает номер текущего поколения.
	 *
	 * @return номер текущего поколения (0 — начальное поле)
	 */
	public int getGeneration() {
		return generation;
	}

	/**
	 * Возвращает размер поля.
	 *
	 * @return ширина (и высота) поля
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Выполняет один шаг автомата: из поколения t получает поколение t+1.
	 * Обновление синхронное: окрестности читаются только из старого поля (cells),
	 * результат пишется в новое (next), затем массивы меняются местами.
	 * После шага проверяется, не повторилось ли поле (поиск цикла).
	 */
	public void step() {
		// Начальное поле запоминаем лениво, перед первым шагом: к этому моменту
		// начальные условия (случайные, ручные и т.д.) уже точно заданы.
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

	// ---------------- обнаружение цикла ----------------

	/**
	 * Проверяет, встречалось ли текущее поле раньше.
	 * Если поле S_generation совпало с S_prev, то предпериод = prev, период = generation - prev.
	 */
	private void checkCycle() {
		if (cycleFound) {
			return; // цикл уже найден, дальше хранить поля незачем
		}
		Integer prev = history.putIfAbsent(snapshot(), generation);
		if (prev != null) {
			cycleFound = true;
			transientLength = prev;
			period = generation - prev;
		}
	}

	/**
	 * Копия поля в виде набора битов: клетка (row, col) -> бит row * size + col.
	 * У BitSet equals/hashCode сравнивают содержимое, поэтому его можно использовать как ключ HashMap.
	 *
	 * @return снимок текущего поля
	 */
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

	/**
	 * Сообщает, найден ли цикл.
	 *
	 * @return true, если поле уже повторилось и цикл найден
	 */
	public boolean isCycleFound() {
		return cycleFound;
	}

	/**
	 * Возвращает длину предпериода (переходного процесса).
	 *
	 * @return предпериод t — номер поколения, с которого начинается цикл
	 * (имеет смысл, только если {@link #isCycleFound()})
	 */
	public int getTransientLength() {
		return transientLength;
	}

	/**
	 * Возвращает период найденного цикла.
	 *
	 * @return период цикла p, 1 — неподвижная точка
	 * (имеет смысл, только если {@link #isCycleFound()})
	 */
	public int getPeriod() {
		return period;
	}

	/**
	 * Считает число живых клеток.
	 *
	 * @return количество живых клеток на поле
	 */
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

	/**
	 * Делает все клетки мёртвыми.
	 */
	public void clear() {
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				cells[row][col] = false;
			}
		}
	}

	/**
	 * Заполняет поле случайно: каждая клетка живая с вероятностью density.
	 * Одинаковый seed всегда даёт одинаковое поле — это нужно для воспроизводимости экспериментов.
	 *
	 * @param density вероятность того, что клетка живая (0..1)
	 * @param seed    начальное значение генератора псевдослучайных чисел
	 */
	public void randomize(double density, long seed) {
		Random random = new Random(seed);
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				cells[row][col] = random.nextDouble() < density;
			}
		}
	}

}
