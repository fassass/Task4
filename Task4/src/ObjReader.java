import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

// Класс для чтения 3D моделей из OBJ файлов
class ObjReader {
    public static Model3D read(File file) throws IOException {
        Model3D model = new Model3D();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("\\s+");
            if (parts.length == 0) continue;

            // Обработка различных типов данных OBJ файла
            switch (parts[0]) {
                case "v":  // Вершина
                    if (parts.length >= 4) {
                        float x = Float.parseFloat(parts[1]);
                        float y = Float.parseFloat(parts[2]);
                        float z = Float.parseFloat(parts[3]);
                        model.getVertices().add(new Vector3(x, y, z));
                    }
                    break;

                case "f":  // Грань (полигон)
                    if (parts.length >= 4) {
                        Face face = new Face();
                        for (int i = 1; i < parts.length; i++) {
                            String[] vertexData = parts[i].split("/");
                            int vertexIndex = Integer.parseInt(vertexData[0]) - 1;
                            face.getVertexIndices().add(vertexIndex);
                        }
                        model.getPolygons().add(face);
                    }
                    break;
            }
        }

        reader.close();
        return model;
    }
}