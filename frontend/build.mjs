import { build } from 'esbuild';
import { mkdir,copyFile } from 'node:fs/promises';
await mkdir('dist',{recursive:true});
await build({entryPoints:['src/main.jsx'],bundle:true,minify:true,outdir:'dist',entryNames:'app',define:{'process.env.NODE_ENV':'"production"'},jsx:'automatic'});
await copyFile('index.html','dist/index.html');
