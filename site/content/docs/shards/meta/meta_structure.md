---
content_type: "doc_shard"
title: "Meta-data object structure"
chapter: "meta"
ordering: 4
label: "meta_structure"
version: 1.1
date: 27.08.2015
published: true
---
<table>
	<tr>
		<td>
			<img src="${img 'meta.svg'}" alt="Meta structure"/>
		</td>
		<td>
			<p>
				The Meta object is a tree-like structure, which can contain other meta objects as branches (which are
				called
				elements) and <a href="#value">Value</a> objects as leafs.
				Both Values and Meta elements are organized in String-keyed maps. And each map element is a list of
				appropriate type. By requesting single Value or Meta element one is supposed to request first element of
				this list.
			</p>
			<hr>
			<p><strong>Note</strong> that such lists are always immutable. Trying to change it may cause a error.</p>
			<hr>
			<p>
				While meta itself does not have any write methods and is considered to be immutable, some of its
				extensions do have methods that can change meta structure. One should be careful not to use mutable meta
				elements when one need immutable one.
			</p>
			<p>
				In order to conveniently edit meta, there is MetaBuilder class.
			</p>
		</td>
	<tr>
</table>

The naming of meta elements and values follows basic DataForge [naming and navigation](#navigation) convention.
Meaning that elements and values could be called like `child_name.grand_child_name.value_name`.
One can event use numbers as queries in such paths like `child_name.grand_child_name[3].value_name`.