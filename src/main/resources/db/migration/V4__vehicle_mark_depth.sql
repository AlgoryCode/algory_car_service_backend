-- Marks placed on the 3D model need a third coordinate; marks on the flat drawings leave it empty.
ALTER TABLE vehicle_marks ADD COLUMN pos_z DOUBLE PRECISION;
