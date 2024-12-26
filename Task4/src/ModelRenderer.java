import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Класс для отрисовки 3D модели
class ModelRenderer extends JPanel {
    private Model3D model;
    private double rotationX = 0;
    private double rotationY = 0;
    private double scale = 100.0;
    private Set<Integer> selectedVertices = new HashSet<>();
    private Set<Integer> selectedPolygons = new HashSet<>();
    private boolean selectingVertices = true;

    // Метод для поворота модели вокруг оси Y
    public void rotateY(double angle) {
        rotationY += angle;
    }

    // Метод для изменения масштаба модели
    public void adjustScale(double factor) {
        scale *= factor;
        scale = Math.max(10, Math.min(500, scale)); // Ограничиваем масштаб
    }
    // Конструктор класса ModelRenderer
    public ModelRenderer() {
        setPreferredSize(new Dimension(500, 500));
        setBackground(Color.WHITE);

        // Обработчик событий мыши
        MouseAdapter mouseHandler = new MouseAdapter() {
            private Point lastPoint;

            @Override
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (model == null) return;

                // Получаем точку клика относительно центра панели
                Point clickPoint = e.getPoint();
                clickPoint.translate(-getWidth() / 2, -getHeight() / 2);

                if (selectingVertices) {
                    // Поиск ближайшей вершины к точке клика
                    int closestVertex = -1;
                    double minDistance = 10;

                    for (int i = 0; i < model.getVertices().size(); i++) {
                        Vector3 vertex = model.getVertices().get(i);
                        double[] projected = project(vertex);
                        double dx = projected[0] * scale - clickPoint.x;
                        double dy = projected[1] * scale - clickPoint.y;
                        double distance = Math.sqrt(dx * dx + dy * dy);

                        if (distance < minDistance) {
                            minDistance = distance;
                            closestVertex = i;
                        }
                    }

                    // Выбор или отмена выбора вершины
                    if (closestVertex != -1) {
                        if (selectedVertices.contains(closestVertex)) {
                            selectedVertices.remove(closestVertex);
                        } else {
                            selectedVertices.add(closestVertex);
                        }
                        repaint();
                    }
                } else {
                    // Выбор полигона по клику
                    int clickedPolygon = -1;

                    for (int i = 0; i < model.getPolygons().size(); i++) {
                        Face face = model.getPolygons().get(i);
                        int[] xPoints = new int[face.getVertexIndices().size()];
                        int[] yPoints = new int[face.getVertexIndices().size()];

                        // Проецируем вершины полигона на экран
                        for (int j = 0; j < face.getVertexIndices().size(); j++) {
                            Vector3 vertex = model.getVertices().get(face.getVertexIndices().get(j));
                            double[] projected = project(vertex);
                            xPoints[j] = (int)(projected[0] * scale) + getWidth() / 2;
                            yPoints[j] = (int)(projected[1] * scale) + getHeight() / 2;
                        }

                        // Проверяем, попадает ли точка клика в полигон
                        if (new Polygon(xPoints, yPoints, face.getVertexIndices().size()).contains(e.getPoint())) {
                            clickedPolygon = i;
                            break;
                        }
                    }

                    // Выбор или отмена выбора полигона
                    if (clickedPolygon != -1) {
                        if (selectedPolygons.contains(clickedPolygon)) {
                            selectedPolygons.remove(clickedPolygon);
                        } else {
                            selectedPolygons.add(clickedPolygon);
                        }
                        repaint();
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastPoint != null) {
                    // Вращение модели при перетаскивании мыши
                    int dx = e.getX() - lastPoint.x;
                    int dy = e.getY() - lastPoint.y;

                    rotationY += dx * 0.01;
                    rotationX += dy * 0.01;

                    lastPoint = e.getPoint();
                    repaint();
                }
            }
        };

        // Добавляем обработчики событий мыши
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        // Добавляем обработчик прокрутки колеса мыши для масштабирования
        addMouseWheelListener(e -> {
            scale += e.getWheelRotation() * -5.0;
            scale = Math.max(10, Math.min(500, scale));
            repaint();
        });
    }
    // Методы установки параметров модели
    public void setModel(Model3D model) {
        this.model = model;
        repaint();
    }

    public void setSelectingVertices(boolean selectingVertices) {
        this.selectingVertices = selectingVertices;
    }

    public void setSelectedVertices(Set<Integer> selectedVertices) {
        this.selectedVertices = selectedVertices;
        repaint();
    }

    public void setSelectedPolygons(Set<Integer> selectedPolygons) {
        this.selectedPolygons = selectedPolygons;
        repaint();
    }

    // Отрисовка модели
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (model == null) return;

        // Настройка графического контекста
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.translate(getWidth() / 2, getHeight() / 2);

        // Отрисовка полигонов
        for (int i = 0; i < model.getPolygons().size(); i++) {
            Face face = model.getPolygons().get(i);
            List<Integer> indices = face.getVertexIndices();
            if (indices.size() < 3) continue;

            // Преобразование 3D координат в 2D
            int[] xPoints = new int[indices.size()];
            int[] yPoints = new int[indices.size()];

            for (int j = 0; j < indices.size(); j++) {
                Vector3 vertex = model.getVertices().get(indices.get(j));
                double[] projectedPoint = project(vertex);
                xPoints[j] = (int)(projectedPoint[0] * scale);
                yPoints[j] = (int)(projectedPoint[1] * scale);
            }

            // Заливка полигона
            if (selectedPolygons.contains(i)) {
                g2d.setColor(new Color(255, 0, 0, 128)); // Полупрозрачный красный для выбранных
            } else {
                g2d.setColor(new Color(200, 200, 255, 128)); // Полупрозрачный синий для обычных
            }
            g2d.fillPolygon(xPoints, yPoints, indices.size());

            // Отрисовка контура полигона
            if (selectedPolygons.contains(i)) {
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(2));
            } else {
                g2d.setColor(Color.BLUE);
                g2d.setStroke(new BasicStroke(1));
            }
            g2d.drawPolygon(xPoints, yPoints, indices.size());
        }

        // Отрисовка вершин
        g2d.setStroke(new BasicStroke(1));
        for (int i = 0; i < model.getVertices().size(); i++) {
            Vector3 vertex = model.getVertices().get(i);
            double[] projectedPoint = project(vertex);
            int x = (int)(projectedPoint[0] * scale);
            int y = (int)(projectedPoint[1] * scale);

            // Выделение выбранных вершин
            if (selectedVertices.contains(i)) {
                g2d.setColor(Color.RED);
                g2d.fillOval(x - 4, y - 4, 8, 8);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillOval(x - 2, y - 2, 4, 4);
            }
        }
    }
    // Проекция 3D точки на 2D плоскость
    private double[] project(Vector3 vertex) {
        double x = vertex.x;
        double y = vertex.y;
        double z = vertex.z;

        // Применяем поворот вокруг оси Y
        double newX = x * Math.cos(rotationY) - z * Math.sin(rotationY);
        double newZ = x * Math.sin(rotationY) + z * Math.cos(rotationY);

        // Применяем поворот вокруг оси X
        double finalY = y * Math.cos(rotationX) - newZ * Math.sin(rotationX);
        double finalZ = y * Math.sin(rotationX) + newZ * Math.cos(rotationX);

        // Применяем перспективную проекцию
        double depth = 5.0;
        double scale = depth / (depth + finalZ);

        return new double[] { newX * scale, finalY * scale };
    }
}