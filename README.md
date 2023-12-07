# MR-loader

> [!IMPORTANT]  
> Crucial information. Tool is fully dependent on server response. Servers may differ and changed overtime.
> If you encounter errors in results, please report.

Tool for loading merge requests from Gerrit. Github functionality will be added later.

MR-loader workflow:

* Identify `maxId` (the largest existing ID number of pull request). It's done by requesting a batch of most recent pull
  requests.
* Iterate over all integers until `maxID` and send the requests via REST API to load all required information of code
  changes with the specified ID.
  Unmodified responses from server stored in `work_dir/gerrit/url_dir/changes` directory where each file map with
  following structure `map[changeID] = jsonResponse`.
* Iterate over all loaded changes in `work_dir/gerrit/url_dir/changes` and load comments if needed
  to `work_dir/gerrit/url_dir/comments`.
  Files in `work_dir/gerrit/url_dir/comments` include map with following structure `map[changeID] = jsonResponse`.
* Iterate over all loaded changes in `work_dir/gerrit/url_dir/changes` and comments
  in `work_dir/gerrit/url_dir/comments` to create a dataset in
  `work_dir/dataset/gerrit/`

## How to use

### Docker

You can use tool as CLI via docker.

First choose working folder for storing all the results. You can set it via mounting volume to `/root` of docker
container
`--volume ~/your/folder/path:/root`. After you need to call the `GerritLoad` command with arguments. List of
arguments you can get via:

```shell scrip
docker run --volume ~/your/folder/path:/root -it ghcr.io/jetbrains-research/mr-loader/mr-loader:latest GerritLoad -h
```

Example run to load all available changes from http://review.openstack.org :

```shell script
docker run --volume ~/your/folder/path:/root -it ghcr.io/jetbrains-research/mr-loader/mr-loader:latest GerritLoad --url http://review.openstack.org
```

## Dataset format

All dataset parts stored in `work_dir/dataset/gerrit/`

### Changes

Stored in `work_dir/dataset/gerrit/changes`.

| created_at | number | key_user | status | comment | key_change | updated_time | subject |
|------------|--------|----------|--------|---------|------------|--------------|---------|
| ...        | ...    | ...      | ...    | ...     | ...        | ...          | ...     |

### Changes files

Stored in `work_dir/dataset/gerrit/changes_files`

| key_change | key_file | 
|------------|----------|
| ...        | ...      |

### Changes reviewer

Stored in `work_dir/dataset/gerrit/changes_files`

| key_change | key_user |
|------------|----------|
| ...        | ...      |

### Commits

Stored in `work_dir/dataset/gerrit/commits`

| oid | committed_date | key_commit | key_change |
|-----|----------------|------------|------------|
| ... | ...            | ...        | ...        |

### Commits author

Stored in `work_dir/dataset/gerrit/commits_author`

| key_commit | author_key_user | committer_key_user | uploader_key_user |
|------------|-----------------|--------------------|-------------------|
| ...        | ...             | ...                | ...               |

### Commits file

Stored in `work_dir/dataset/gerrit/commits_file`

| key_commit | key_file | lines_inserted | lines_deleted	 | size	 | size_delta | status |
|------------|----------|----------------|----------------|-------|------------|--------|
| ...        | ...      | ...            | ...            | ...   | ...        | ...    | 

### Files

Stored in `work_dir/dataset/gerrit/files`

| path | key |
|------|-----|
| ...  | ... |

### Users

Stored in `work_dir/dataset/gerrit/users`

| name | email | login |
|------|-------|-------|
| ...  | ...   | ...   |
