package ai.tambo.foundation.ngoma

import ai.tambo.foundation.ngoma.models.Note
import ai.tambo.tmodeler.db.Tdb
import ai.tambo.tmodeler.extension.synchronization.core.TServerConfigBase
import ai.tambo.tmodeler.modeler.TModeler
import java.io.File

object NGomaTm {
    // Incrémenter pour repartir d'une base vide.
    const val DB_VERSION = 1

    fun start() {
        File("./data/").mkdirs()
        TModeler.start().connect(
            Tdb.Builder()
                .type(Tdb.Type.SQLITE)
                .dbDir("./data/")
                .dbName("ngoma-$DB_VERSION")
                .server(TServerConfigBase())
                .accept(Note().clazz().getPackage())
                .start()!!
        )
        Note.tms.commit()
    }
}
