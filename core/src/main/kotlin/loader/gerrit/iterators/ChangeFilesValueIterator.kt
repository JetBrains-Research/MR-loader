package loader.gerrit.iterators

import entity.rest.gerrit.ChangeGerrit
import java.io.File

class ChangeFilesValueIterator(files: Sequence<File>) :
  FilesIdToValueIterator<ChangeGerrit>(files, ChangeGerrit.serializer())