package loader.gerrit.iterators

import entity.rest.gerrit.ChangeMetaData
import java.io.File

class ChangesMetaDataFilesIterator(files: Sequence<File>) :
  FilesListIterator<ChangeMetaData>(files, ChangeMetaData.serializer())
