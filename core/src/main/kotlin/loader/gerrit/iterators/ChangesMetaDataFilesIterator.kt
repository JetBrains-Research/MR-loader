package loader.gerrit.iterators

import client.ClientGerritREST
import entity.rest.gerrit.ChangeMetaData
import java.io.File

class ChangesMetaDataFilesIterator(files: Sequence<File>) :
  FilesListIterator<ChangeMetaData>(files, ClientGerritREST.json, ChangeMetaData.serializer())
