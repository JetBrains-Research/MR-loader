package loader.gerrit.iterators

import client.ClientGerritREST
import entity.rest.gerrit.ChangeGerrit
import java.io.File

class ChangeFilesValueIterator(files: Sequence<File>) :
  FilesValueIterator<ChangeGerrit>(files, ClientGerritREST.json, ChangeGerrit.serializer())