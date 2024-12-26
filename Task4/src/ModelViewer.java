import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

// Основной класс приложения, наследуется от JFrame для создания окна
public class ModelViewer extends JFrame {
    // Список всех загруженных 3D моделей
    private List<Model3D> models;
    // Текущая активная модель для редактирования
    private Model3D activeModel;
    // Компонент для отрисовки 3D модели
    private ModelRenderer renderer;
    // Панель для размещения компонентов интерфейса
    private JPanel modelPanel;
    // Список моделей в интерфейсе
    private JList<String> modelList;
    // Флаг для отслеживания текущей темы (светлая/темная)
    private boolean isDarkTheme = false;
    // Множество выбранных вершин модели
    private Set<Integer> selectedVertices = new HashSet<>();
    // Множество выбранных полигонов модели
    private Set<Integer> selectedPolygons = new HashSet<>();
    // Флаг режима выбора (вершины/полигоны)
    private boolean selectingVertices = true;
    // Таймер для автоматического вращения модели
    private Timer rotationTimer;
    // Флаг вращения модели
    private boolean isRotating = false;

    // Конструктор класса
    public ModelViewer() {
        models = new ArrayList<>();
        setupUI();
    }

    // Метод настройки пользовательского интерфейса
    private void setupUI() {
        setTitle("3D Model Viewer");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        modelPanel = new JPanel(new BorderLayout());
        setContentPane(modelPanel);

        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);

        JPanel modelListPanel = createModelListPanel();
        add(modelListPanel, BorderLayout.WEST);

        renderer = new ModelRenderer();
        add(renderer, BorderLayout.CENTER);

        applyTheme(isDarkTheme);
    }

    // Создание панели меню
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem loadItem = new JMenuItem("Load Model");
        loadItem.addActionListener(e -> loadModel());

        JMenuItem saveItem = new JMenuItem("Save Model");
        saveItem.addActionListener(e -> saveModel());

        fileMenu.add(loadItem);
        fileMenu.add(saveItem);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem deleteItem = new JMenuItem("Delete Selected");
        deleteItem.addActionListener(e -> deleteSelectedParts());

        editMenu.add(deleteItem);

        JMenu viewMenu = new JMenu("View");
        JMenuItem themeItem = new JMenuItem("Toggle Theme");
        themeItem.addActionListener(e -> toggleTheme());

        viewMenu.add(themeItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);

        return menuBar;
    }

    // Создание панели инструментов
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Создаем кнопки выбора режима (вершины/полигоны)
        JToggleButton vertexMode = new JToggleButton("Select Vertices");
        JToggleButton polygonMode = new JToggleButton("Select Polygons");
        ButtonGroup group = new ButtonGroup();
        group.add(vertexMode);
        group.add(polygonMode);
        vertexMode.setSelected(true);

        // Добавляет оброботчики для кнопок режима
        vertexMode.addActionListener(e -> {
            selectingVertices = true;
            renderer.setSelectingVertices(true);
        });
        polygonMode.addActionListener(e -> {
            selectingVertices = false;
            renderer.setSelectingVertices(false);
        });

        JButton rotateButton = new JButton("Rotate");
        JButton scaleButton = new JButton("Scale");

        // Оброботчики для кнопок управления
        rotateButton.addActionListener(e -> rotateActiveModel());
        scaleButton.addActionListener(e -> scaleActiveModel());

        toolBar.add(vertexMode);
        toolBar.add(polygonMode);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(rotateButton);
        toolBar.add(scaleButton);

        return toolBar;
    }
    // Панель со списком моделей
    private JPanel createModelListPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Models"));

        // Создание модели списка для хранения названий моделей
        DefaultListModel<String> listModel = new DefaultListModel<>();
        modelList = new JList<>(listModel);
        modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Это обработчик выбора модели из мписка
        modelList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = modelList.getSelectedIndex();
                if (index >= 0 && index < models.size()) {
                    setActiveModel(models.get(index));
                }
            }
        });

        panel.add(new JScrollPane(modelList), BorderLayout.CENTER);
        return panel;
    }

    // Загрузка 3D модели из файла
    private void loadModel() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                // Чтение модели из файла
                Model3D model = ObjReader.read(file);
                models.add(model);
                setActiveModel(model);
                updateModelList();
            }
        } catch (Exception e) {
            showError("Error loading model", e.getMessage());
        }
    }

    // Сохранение модели в файл
    private void saveModel() {
        if (activeModel == null) {
            showError("Error", "No active model selected");
            return;
        }

        try {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                // Записывает модель в файл
                ObjWriter.write(activeModel, file);
            }
        } catch (Exception e) {
            showError("Error saving model", e.getMessage());
        }
    }

    // Удаление выбранных элементов модели (вершин или полигонов)
    private void deleteSelectedParts() {
        if (activeModel == null) {
            showError("Error", "No active model selected");
            return;
        }

        // Удаление выбранных вершин
        if (!selectedVertices.isEmpty()) {
            List<Vector3> newVertices = new ArrayList<>();
            Map<Integer, Integer> oldToNewIndices = new HashMap<>();

            // Новый список вершин без удаленных
            for (int i = 0; i < activeModel.getVertices().size(); i++) {
                if (!selectedVertices.contains(i)) {
                    oldToNewIndices.put(i, newVertices.size());
                    newVertices.add(activeModel.getVertices().get(i));
                }
            }

            // Обновляет индексы в полигонах
            List<Face> newPolygons = new ArrayList<>();
            for (Face face : activeModel.getPolygons()) {
                List<Integer> newIndices = new ArrayList<>();
                boolean faceValid = true;

                for (Integer oldIndex : face.getVertexIndices()) {
                    if (oldToNewIndices.containsKey(oldIndex)) {
                        newIndices.add(oldToNewIndices.get(oldIndex));
                    } else {
                        faceValid = false;
                        break;
                    }
                }

                if (faceValid && newIndices.size() >= 3) {
                    Face newFace = new Face();
                    newFace.getVertexIndices().addAll(newIndices);
                    newPolygons.add(newFace);
                }
            }

            // Обновляет модель
            activeModel.getVertices().clear();
            activeModel.getVertices().addAll(newVertices);
            activeModel.getPolygons().clear();
            activeModel.getPolygons().addAll(newPolygons);
        }

        // Удаление выбранных полигонов
        if (!selectedPolygons.isEmpty()) {
            List<Face> newPolygons = new ArrayList<>();
            for (int i = 0; i < activeModel.getPolygons().size(); i++) {
                if (!selectedPolygons.contains(i)) {
                    newPolygons.add(activeModel.getPolygons().get(i));
                }
            }
            activeModel.getPolygons().clear();
            activeModel.getPolygons().addAll(newPolygons);
        }

        // Очищаем выбранные элементы и обновляет отображение
        selectedVertices.clear();
        selectedPolygons.clear();
        renderer.setSelectedVertices(selectedVertices);
        renderer.setSelectedPolygons(selectedPolygons);
        renderer.repaint();
    }
    // Тема интерфейса
    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        applyTheme(isDarkTheme);
    }


    private void applyTheme(boolean isDark) {
        Color bgColor = isDark ? new Color(43, 43, 43) : new Color(240, 240, 240);
        Color fgColor = isDark ? new Color(255, 255, 255) : new Color(0, 0, 0);

        UIManager.put("Panel.background", bgColor);
        UIManager.put("Menu.foreground", fgColor);
        UIManager.put("MenuItem.foreground", fgColor);

        SwingUtilities.updateComponentTreeUI(this);
        renderer.setBackground(isDark ? Color.DARK_GRAY : Color.WHITE);
        renderer.repaint();
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    // Обновление списка моделей в интерфейсе
    private void updateModelList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (int i = 0; i < models.size(); i++) {
            listModel.addElement("Model " + (i + 1));
        }
        modelList.setModel(listModel);
    }

    // Установка активной модели
    private void setActiveModel(Model3D model) {
        activeModel = model;
        renderer.setModel(model);
        selectedVertices.clear();
        selectedPolygons.clear();
        renderer.setSelectedVertices(selectedVertices);
        renderer.setSelectedPolygons(selectedPolygons);
    }

    // Включение или выключение автоматического вращения моделт
    private void rotateActiveModel() {
        if (rotationTimer != null && rotationTimer.isRunning()) {
            rotationTimer.stop();
            isRotating = false;
        } else {
            isRotating = true;
            rotationTimer = new Timer(50, e -> {
                renderer.rotateY(0.02);
                renderer.repaint();
            });
            rotationTimer.start();
        }
    }

    // Изменение масштаба модели
    private void scaleActiveModel() {
        renderer.adjustScale(1.2); // Увеличиваем масштаб на 20%
        renderer.repaint();
    }

    // Точка входа в приложение
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ModelViewer viewer = new ModelViewer();
            viewer.setVisible(true);
        });
    }
}